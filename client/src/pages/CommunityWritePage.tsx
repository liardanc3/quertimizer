import {
  type ChangeEvent,
  type ClipboardEvent,
  type KeyboardEvent,
  type MouseEvent,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import {
  clearCommunityEditorDraft,
  getCommunityEditorDraft,
  saveCommunityEditorDraft,
} from '../lib/communityStore';
import {
  createCommunityPost,
  fetchCommunityPostDetail,
  fetchCommunityTagSuggestions,
  updateCommunityPost,
  type CommunityTagSuggestion,
} from '../lib/communityApi';
import { COMMUNITY_PATH, getCommunityPostPath, navigate } from '../lib/navigation';
import './CommunityPage.css';

interface CommunityWritePageProps {
  postId?: string;
}

interface EditorValues {
  title: string;
  draftTag: string;
  selectedTags: string[];
  contentHtml: string;
}

function normalizeKeyword(value: string) {
  return value
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[_\-\s]+/g, '')
    .replace(/[^\p{L}\p{N}]/gu, '');
}

function escapeHtmlAttribute(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function hasMeaningfulHtml(value: string) {
  const withoutTags = value
    .replace(/<img[\s\S]*?>/gi, ' ')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  return withoutTags.length > 0 || /<img[\s\S]*?>/i.test(value);
}

function createEmptyValues(): EditorValues {
  return {
    title: '',
    draftTag: '',
    selectedTags: [],
    contentHtml: '',
  };
}

export default function CommunityWritePage({ postId }: CommunityWritePageProps) {
  const draftKey = postId ? `community-edit-${postId}` : 'community-write';
  const [title, setTitle] = useState('');
  const [draftTag, setDraftTag] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [editorHtml, setEditorHtml] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [draftRecoveredAt, setDraftRecoveredAt] = useState<string | null>(null);
  const [tagSuggestions, setTagSuggestions] = useState<CommunityTagSuggestion[]>([]);
  const [isLoading, setIsLoading] = useState(Boolean(postId));
  const [notFound, setNotFound] = useState(false);

  const editorRef = useRef<HTMLDivElement | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const savedRangeRef = useRef<Range | null>(null);
  const hydratedRef = useRef(false);

  const normalizedDraftTag = normalizeKeyword(draftTag);
  const isEditorEmpty = !hasMeaningfulHtml(editorHtml);

  useEffect(() => {
    if (!postId) {
      const savedDraft = getCommunityEditorDraft(draftKey);
      const nextValues = savedDraft
        ? {
            title: savedDraft.title,
            draftTag: savedDraft.draftTag,
            selectedTags: savedDraft.selectedTags,
            contentHtml: savedDraft.contentHtml,
          }
        : createEmptyValues();

      setTitle(nextValues.title);
      setDraftTag(nextValues.draftTag);
      setSelectedTags(nextValues.selectedTags);
      setEditorHtml(nextValues.contentHtml);
      setDraftRecoveredAt(savedDraft?.updatedAt ?? null);
      hydratedRef.current = true;

      window.requestAnimationFrame(() => {
        if (editorRef.current) {
          editorRef.current.innerHTML = nextValues.contentHtml;
        }
      });
      return;
    }

    let cancelled = false;
    setIsLoading(true);

    fetchCommunityPostDetail(postId)
      .then((post) => {
        if (cancelled) {
          return;
        }

        const savedDraft = getCommunityEditorDraft(draftKey);
        const baseValues: EditorValues = {
          title: post.title,
          draftTag: '',
          selectedTags: post.tags,
          contentHtml: post.contentHtml,
        };
        const nextValues = savedDraft
          ? {
              title: savedDraft.title,
              draftTag: savedDraft.draftTag,
              selectedTags: savedDraft.selectedTags,
              contentHtml: savedDraft.contentHtml,
            }
          : baseValues;

        setTitle(nextValues.title);
        setDraftTag(nextValues.draftTag);
        setSelectedTags(nextValues.selectedTags);
        setEditorHtml(nextValues.contentHtml);
        setDraftRecoveredAt(savedDraft?.updatedAt ?? null);
        hydratedRef.current = true;
        setNotFound(false);

        window.requestAnimationFrame(() => {
          if (editorRef.current) {
            editorRef.current.innerHTML = nextValues.contentHtml;
          }
        });
      })
      .catch(() => {
        if (!cancelled) {
          setNotFound(true);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [draftKey, postId]);

  useEffect(() => {
    if (!hydratedRef.current) {
      return;
    }

    const hasContent = title.trim() || draftTag.trim() || selectedTags.length > 0 || hasMeaningfulHtml(editorHtml);

    if (!hasContent) {
      clearCommunityEditorDraft(draftKey);
      return;
    }

    const timeoutId = window.setTimeout(() => {
      saveCommunityEditorDraft(draftKey, {
        title,
        draftTag,
        selectedTags,
        contentHtml: editorHtml,
      });
    }, 250);

    return () => window.clearTimeout(timeoutId);
  }, [draftKey, draftTag, editorHtml, selectedTags, title]);

  useEffect(() => {
    if (!normalizedDraftTag) {
      setTagSuggestions([]);
      return;
    }

    let cancelled = false;

    fetchCommunityTagSuggestions(draftTag)
      .then((nextTagSuggestions) => {
        if (!cancelled) {
          setTagSuggestions(nextTagSuggestions);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setTagSuggestions([]);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [draftTag, normalizedDraftTag]);

  if (isLoading) {
    return (
      <div className="page-stack community-write-root">
        <section className="panel-card community-detail-card">
          <p className="panel-meta">커뮤니티</p>
          <h1 className="page-title">글 정보를 불러오는 중이다.</h1>
        </section>
      </div>
    );
  }

  if (postId && notFound) {
    return (
      <div className="page-stack community-write-root">
        <section className="panel-card community-detail-card">
          <button type="button" className="btn ghost community-back-button" onClick={() => navigate(COMMUNITY_PATH)}>
            뒤로가기
          </button>
          <p className="panel-meta">커뮤니티</p>
          <h1 className="page-title">수정할 게시글을 찾을 수 없다.</h1>
        </section>
      </div>
    );
  }

  function handleCancel() {
    if (window.history.state?.from) {
      window.history.back();
      return;
    }

    navigate(postId ? getCommunityPostPath(postId) : COMMUNITY_PATH);
  }

  function rememberSelection() {
    const selection = window.getSelection();
    const editor = editorRef.current;

    if (!selection || !editor || selection.rangeCount === 0) {
      return;
    }

    const range = selection.getRangeAt(0);

    if (!editor.contains(range.commonAncestorContainer)) {
      return;
    }

    savedRangeRef.current = range.cloneRange();
  }

  function placeCaretAtEnd() {
    const editor = editorRef.current;
    const selection = window.getSelection();

    if (!editor || !selection) {
      return;
    }

    const range = document.createRange();
    range.selectNodeContents(editor);
    range.collapse(false);
    selection.removeAllRanges();
    selection.addRange(range);
    savedRangeRef.current = range.cloneRange();
  }

  function restoreSelection() {
    const editor = editorRef.current;
    const selection = window.getSelection();

    if (!editor || !selection) {
      return;
    }

    editor.focus();

    if (!savedRangeRef.current) {
      placeCaretAtEnd();
      return;
    }

    try {
      selection.removeAllRanges();
      selection.addRange(savedRangeRef.current);
    } catch {
      placeCaretAtEnd();
    }
  }

  function syncEditorHtml() {
    const nextHtml = editorRef.current?.innerHTML ?? '';
    setEditorHtml(nextHtml);
    rememberSelection();
    setFeedback(null);
  }

  function runEditorCommand(command: string, value?: string) {
    restoreSelection();
    document.execCommand(command, false, value);
    syncEditorHtml();
  }

  function insertImage(source: string, altText: string) {
    restoreSelection();
    document.execCommand(
      'insertHTML',
      false,
      `<figure class="community-editor-figure"><img src="${escapeHtmlAttribute(source)}" alt="${escapeHtmlAttribute(altText)}" /></figure><p><br /></p>`,
    );
    syncEditorHtml();
  }

  function readAndInsertImage(file: File) {
    if (!file.type.startsWith('image/')) {
      setFeedback('이미지 파일만 첨부할 수 있다.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result !== 'string') {
        return;
      }

      insertImage(reader.result, file.name || '첨부 이미지');
    };
    reader.readAsDataURL(file);
  }

  function handleAddTag(tagLabel: string) {
    const trimmedTagLabel = tagLabel.trim();
    const normalizedLabel = normalizeKeyword(trimmedTagLabel);

    if (!normalizedLabel) {
      return;
    }

    setSelectedTags((currentTags) =>
      currentTags.some((currentTag) => normalizeKeyword(currentTag) === normalizedLabel)
        ? currentTags
        : [...currentTags, trimmedTagLabel],
    );
    setDraftTag('');
    setFeedback(null);
  }

  function handleAddDraftTag() {
    handleAddTag(draftTag);
  }

  function handleRemoveTag(tagLabel: string) {
    setSelectedTags((currentTags) => currentTags.filter((currentTag) => currentTag !== tagLabel));
  }

  function handleTagKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      event.preventDefault();
      handleAddDraftTag();
    }
  }

  function handleToolbarMouseDown(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault();
  }

  function handleImageFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    readAndInsertImage(file);
    event.target.value = '';
  }

  function handleEditorPaste(event: ClipboardEvent<HTMLDivElement>) {
    const imageItem = Array.from(event.clipboardData.items).find((item) => item.type.startsWith('image/'));
    const file = imageItem?.getAsFile();

    if (!file) {
      return;
    }

    event.preventDefault();
    rememberSelection();
    readAndInsertImage(file);
  }

  function handleResetToSavedSource() {
    clearCommunityEditorDraft(draftKey);
    setDraftRecoveredAt(null);
    window.location.reload();
  }

  function handleClearDraft() {
    clearCommunityEditorDraft(draftKey);
    setDraftRecoveredAt(null);
    setFeedback('임시 저장을 비웠다.');
  }

  async function handleSubmit() {
    if (!title.trim() || !hasMeaningfulHtml(editorHtml)) {
      setFeedback('제목과 본문은 반드시 입력해야 한다.');
      return;
    }

    try {
      let savedPostId = postId;

      if (postId) {
        await updateCommunityPost(postId, {
          title,
          tags: selectedTags,
          contentHtml: editorHtml,
        });
      } else {
        savedPostId = await createCommunityPost({
          title,
          tags: selectedTags,
          contentHtml: editorHtml,
        });
      }

      clearCommunityEditorDraft(draftKey);
      navigate(getCommunityPostPath(savedPostId!), {
        state: {
          from: window.history.state?.from ?? COMMUNITY_PATH,
        },
      });
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '게시글 저장에 실패했다.');
    }
  }

  const suggestedTags = useMemo(
    () => tagSuggestions.filter((tag) => !selectedTags.some((selectedTag) => normalizeKeyword(selectedTag) === normalizeKeyword(tag.tag))),
    [selectedTags, tagSuggestions],
  );
  const savedDraftLabel = draftRecoveredAt ? new Date(draftRecoveredAt).toLocaleString('ko-KR') : null;
  const pageTitle = postId ? '게시글 수정' : '글쓰기';
  const pageChip = postId ? '글 수정' : '글쓰기';

  return (
    <div className="page-stack community-write-root">
      <section className="panel-card community-write-page">
        <input
          ref={imageInputRef}
          type="file"
          accept="image/*"
          className="community-editor-file-input"
          onChange={handleImageFileChange}
        />

        <div className="community-detail-topbar">
          <button type="button" className="btn ghost community-back-button" onClick={handleCancel}>
            뒤로가기
          </button>
          <span className="subtle-chip">{pageChip}</span>
        </div>

        <div className="community-write-header">
          <p className="panel-meta">커뮤니티</p>
          <h1 className="page-title">{pageTitle}</h1>
          <p className="muted-text">굵게, 인용, 이미지 첨부, 임시 저장 복구까지 같은 화면에서 처리한다.</p>
        </div>

        {savedDraftLabel ? (
          <div className="community-draft-strip">
            <div className="community-draft-copy">
              <strong>임시 저장을 불러왔다.</strong>
              <span>{savedDraftLabel} 기준 내용으로 이어서 작성 중이다.</span>
            </div>
            <div className="community-draft-actions">
              <button type="button" className="btn ghost" onClick={handleClearDraft}>
                임시 저장 비우기
              </button>
              <button type="button" className="btn secondary" onClick={handleResetToSavedSource}>
                {postId ? '원본으로 되돌리기' : '새 글로 초기화'}
              </button>
            </div>
          </div>
        ) : null}

        <div className="community-write-layout">
          <div className="community-write-main">
            <label className="field-stack">
              <span className="field-label">제목</span>
              <input
                className="text-field"
                value={title}
                onChange={(event) => {
                  setTitle(event.target.value);
                  setFeedback(null);
                }}
                placeholder="제목을 입력해."
              />
            </label>

            <div className="field-stack">
              <span className="field-label">태그</span>

              <div className="community-tag-input-stack">
                <div className="community-tag-input-row">
                  <input
                    className="text-field community-tag-input-field"
                    value={draftTag}
                    onChange={(event) => setDraftTag(event.target.value)}
                    onKeyDown={handleTagKeyDown}
                    placeholder="예: left_join, 00001-00001"
                  />
                  <button
                    type="button"
                    className="btn secondary community-tag-submit-button"
                    onClick={handleAddDraftTag}
                    disabled={!draftTag.trim()}
                    aria-label="입력한 태그 추가"
                    title="입력한 태그 추가"
                  >
                    +
                  </button>
                </div>

                {draftTag.trim() ? (
                  <div className="community-tag-related-box">
                    <div className="community-tag-related-head">
                      <span className="community-tag-related-title">관련 태그</span>
                      <span className="community-tag-related-caption">입력값과 비슷한 태그를 보여준다.</span>
                    </div>

                    {suggestedTags.length > 0 ? (
                      <div className="community-tag-related-list">
                        {suggestedTags.map((tag) => (
                          <button
                            key={tag.tag}
                            type="button"
                            className="community-tag-related-item"
                            onClick={() => handleAddTag(tag.tag)}
                          >
                            <span className="community-tag-related-name">#{tag.tag}</span>
                            <span className="community-tag-related-desc">사용 {tag.usageCount}회</span>
                          </button>
                        ))}
                      </div>
                    ) : (
                      <p className="community-tag-related-empty">비슷한 태그가 없으면 새 태그로 바로 추가된다.</p>
                    )}
                  </div>
                ) : (
                  <p className="hint-text community-tag-helper">태그를 1글자 이상 입력하면 비슷한 태그가 아래에 나온다.</p>
                )}
              </div>

              {selectedTags.length > 0 ? (
                <div className="community-write-tag-list">
                  {selectedTags.map((tag) => (
                    <button
                      key={tag}
                      type="button"
                      className="community-selected-tag"
                      onClick={() => handleRemoveTag(tag)}
                    >
                      <span>#{tag}</span>
                      <span aria-hidden="true">x</span>
                    </button>
                  ))}
                </div>
              ) : null}
            </div>

            <div className="field-stack">
              <div className="community-editor-label-row">
                <span className="field-label">본문</span>
                <span className="hint-text">굵게, 인용, 링크, 이미지 첨부</span>
              </div>

              <div className="community-editor-shell">
                <div className="community-editor-toolbar">
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => runEditorCommand('bold')}
                  >
                    B
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => runEditorCommand('formatBlock', 'blockquote')}
                  >
                    "
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => imageInputRef.current?.click()}
                  >
                    이미지
                  </button>
                </div>

                <div
                  ref={editorRef}
                  className={`community-editor-body ${isEditorEmpty ? 'is-empty' : ''}`.trim()}
                  contentEditable
                  suppressContentEditableWarning
                  onInput={syncEditorHtml}
                  onBlur={rememberSelection}
                  onKeyUp={rememberSelection}
                  onMouseUp={rememberSelection}
                  onPaste={handleEditorPaste}
                  data-placeholder="본문을 입력해."
                />
              </div>
            </div>

            {feedback ? <p className="community-editor-feedback">{feedback}</p> : null}

            <div className="community-write-actions">
              <button type="button" className="btn ghost" onClick={handleCancel}>
                취소
              </button>
              <button type="button" className="btn primary" onClick={handleSubmit}>
                {postId ? '수정 저장' : '등록'}
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
