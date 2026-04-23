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
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import {
  createCommunityPost,
  fetchCommunityPostDetail,
  fetchCommunityTagSuggestions,
  updateCommunityPost,
  type CommunityTagSuggestion,
} from '../lib/communityApi';
import { COMMUNITY_PATH, getCommunityPostPath, navigate } from '../lib/navigation';
import { openLoginOverlay, setLoginOverlayDescription } from '../lib/authOverlay';
import { useMockSession } from '../lib/session';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import './CommunityPage.css';

interface CommunityWritePageProps {
  postId?: string;
  embedded?: boolean;
}

interface EditorValues {
  title: string;
  draftTag: string;
  selectedTags: string[];
  contentHtml: string;
}

interface CommunityWriteFavoriteSnapshot extends EditorValues {
  postId?: string;
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

function BoldToolIcon() {
  return <span aria-hidden="true">B</span>;
}

function UnderlineToolIcon() {
  return <span aria-hidden="true" className="community-editor-tool-underlined">U</span>;
}

function QuoteToolIcon() {
  return <span aria-hidden="true">"</span>;
}

function CodeToolIcon() {
  return <span aria-hidden="true">&lt;/&gt;</span>;
}

function ImageToolIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <rect x="2.2" y="3" width="11.6" height="10" rx="1.2" stroke="currentColor" strokeWidth="1.3" />
      <circle cx="5.5" cy="6.3" r="1.1" fill="currentColor" />
      <path d="m4.1 11 2.7-2.8 2.2 2.2 1.4-1.4L12 11" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function DisclosureToolIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.3 5.2h9.4" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" />
      <path d="m6 7.2 2 2 2-2" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M3.3 11.1h9.4" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" />
    </svg>
  );
}

function TagRemoveIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="m4.4 4.4 7.2 7.2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="m11.6 4.4-7.2 7.2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

function createEmptyValues(): EditorValues {
  return {
    title: '',
    draftTag: '',
    selectedTags: [],
    contentHtml: '',
  };
}

export default function CommunityWritePage({ postId, embedded = false }: CommunityWritePageProps) {
  const { isAuthenticated } = useMockSession();
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<CommunityWriteFavoriteSnapshot>('communityWrite'), []);
  const draftKey = postId ? `community-edit-${postId}` : 'community-write';
  const pageChip = postId ? '글 수정' : '글쓰기';
  const [title, setTitle] = useState('');
  const [draftTag, setDraftTag] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [editorHtml, setEditorHtml] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [tagSuggestions, setTagSuggestions] = useState<CommunityTagSuggestion[]>([]);
  const [isLoading, setIsLoading] = useState(Boolean(postId));
  const [notFound, setNotFound] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const editorRef = useRef<HTMLDivElement | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const savedRangeRef = useRef<Range | null>(null);
  const hydratedRef = useRef(false);

  const normalizedDraftTag = normalizeKeyword(draftTag);
  const isEditorEmpty = !hasMeaningfulHtml(editorHtml);
  const hasPostDraft = title.trim() !== '' || draftTag.trim() !== '' || selectedTags.length > 0 || !isEditorEmpty;

  useEffect(() => {
    setLoginOverlayDescription(hasPostDraft ? '작성 중인 게시글은 유지됩니다. 로그인 후 이어서 작성할 수 있습니다.' : null);

    return () => setLoginOverlayDescription(null);
  }, [hasPostDraft]);

  useEffect(() => {
    clearFavoriteRestoreSnapshot('communityWrite');
  }, []);

  useEffect(() => {
    if (!postId) {
      const savedDraft = getCommunityEditorDraft(draftKey);
      const favoriteDraft = favoriteRestoreSnapshot && (favoriteRestoreSnapshot.postId ?? null) === null
        ? {
            title: favoriteRestoreSnapshot.title,
            draftTag: favoriteRestoreSnapshot.draftTag,
            selectedTags: favoriteRestoreSnapshot.selectedTags,
            contentHtml: favoriteRestoreSnapshot.contentHtml,
          }
        : null;
      const nextValues = favoriteDraft
        ?? (savedDraft
          ? {
              title: savedDraft.title,
              draftTag: savedDraft.draftTag,
              selectedTags: savedDraft.selectedTags,
              contentHtml: savedDraft.contentHtml,
            }
          : createEmptyValues());

      setTitle(nextValues.title);
      setDraftTag(nextValues.draftTag);
      setSelectedTags(nextValues.selectedTags);
      setEditorHtml(nextValues.contentHtml);
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
        const favoriteDraft = favoriteRestoreSnapshot && favoriteRestoreSnapshot.postId === postId
          ? {
              title: favoriteRestoreSnapshot.title,
              draftTag: favoriteRestoreSnapshot.draftTag,
              selectedTags: favoriteRestoreSnapshot.selectedTags,
              contentHtml: favoriteRestoreSnapshot.contentHtml,
            }
          : null;
        const nextValues = favoriteDraft
          ?? (savedDraft
            ? {
                title: savedDraft.title,
                draftTag: savedDraft.draftTag,
                selectedTags: savedDraft.selectedTags,
                contentHtml: savedDraft.contentHtml,
              }
            : baseValues);

        setTitle(nextValues.title);
        setDraftTag(nextValues.draftTag);
        setSelectedTags(nextValues.selectedTags);
        setEditorHtml(nextValues.contentHtml);
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
  }, [draftKey, favoriteRestoreSnapshot, postId]);

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
    if (!normalizedDraftTag || selectedTags.length >= 7) {
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
  }, [draftTag, normalizedDraftTag, selectedTags.length]);

  function handleCancel() {
    if (window.history.state?.from) {
      window.history.back();
      return;
    }

    navigate(postId ? getCommunityPostPath(postId) : COMMUNITY_PATH);
  }

  function handleDraftSave() {
    const hasContent = title.trim() || draftTag.trim() || selectedTags.length > 0 || hasMeaningfulHtml(editorHtml);

    if (!hasContent) {
      clearCommunityEditorDraft(draftKey);
      return;
    }

    saveCommunityEditorDraft(draftKey, {
      title,
      draftTag,
      selectedTags,
      contentHtml: editorHtml,
    });
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

  function insertCodeBlock() {
    restoreSelection();
    document.execCommand('insertHTML', false, '<pre><code><br /></code></pre><p><br /></p>');
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

  function insertDisclosureBlock() {
    restoreSelection();
    document.execCommand(
      'insertHTML',
      false,
      '<details class="community-editor-disclosure" open><summary>접고 펼치기</summary><p><br /></p></details><p><br /></p>',
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
    const trimmedTagLabel = tagLabel.trim().replace(/^#/, '');
    const normalizedLabel = normalizeKeyword(trimmedTagLabel);

    if (!normalizedLabel) {
      return;
    }

    if (selectedTags.length >= 7) {
      setFeedback('태그는 최대 7개까지 추가할 수 있다.');
      return;
    }

    setSelectedTags((currentTags) => {
      if (currentTags.length >= 7) {
        return currentTags;
      }

      return currentTags.some((currentTag) => normalizeKeyword(currentTag) === normalizedLabel)
        ? currentTags
        : [...currentTags, trimmedTagLabel];
    });
    setDraftTag('');
    setFeedback(null);
  }

  function handleRemoveTag(tagLabel: string) {
    setSelectedTags((currentTags) => currentTags.filter((currentTag) => currentTag !== tagLabel));
  }

  function handleTagKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      handleAddTag(draftTag);
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

  async function handleSubmit() {
    if (isSubmitting) {
      return;
    }

    if (!isAuthenticated) {
      openLoginOverlay();
      return;
    }

    if (!title.trim() || !hasMeaningfulHtml(editorHtml)) {
      setFeedback('제목과 본문은 반드시 입력해야 한다.');
      return;
    }

    setIsSubmitting(true);

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
    } finally {
      setIsSubmitting(false);
    }
  }

  const suggestedTags = useMemo(
    () => tagSuggestions
      .filter((tag) => !selectedTags.some((selectedTag) => normalizeKeyword(selectedTag) === normalizeKeyword(tag.tag)))
      .slice(0, 7),
    [selectedTags, tagSuggestions],
  );

  function renderWritePanel(includeTopbar: boolean) {
    return (
      <>
        <section className="panel-card community-detail-card community-write-page">
          <input
            ref={imageInputRef}
            type="file"
            accept="image/*"
            className="community-editor-file-input"
            onChange={handleImageFileChange}
          />

          {includeTopbar ? (
            <div className="community-detail-topbar">
              <div className="solve-dbms-tab-row community-detail-tab-row" aria-label="커뮤니티 글쓰기">
                <span className="solve-dbms-tab is-selected community-detail-category-tab">{pageChip}</span>
              </div>
            </div>
          ) : null}

          <div className="community-detail-header community-write-edit-header">
            <input
              type="text"
              value={title}
              onChange={(event) => {
                setTitle(event.target.value);
                setFeedback(null);
              }}
              className="text-field community-detail-title-input"
              placeholder="제목을 입력해."
            />

            <div className="community-detail-edit-tags community-write-edit-tags">
              {selectedTags.length > 0 ? (
                <div className="community-detail-edit-tag-list">
                  {selectedTags.map((tag) => (
                    <button
                      key={tag}
                      type="button"
                      className="community-detail-edit-tag"
                      onClick={() => handleRemoveTag(tag)}
                    >
                      <span>#{tag}</span>
                      <span aria-hidden="true" className="community-detail-edit-tag-remove">
                        <TagRemoveIcon />
                      </span>
                    </button>
                  ))}
                </div>
              ) : null}

              <input
                type="text"
                value={draftTag}
                onChange={(event) => {
                  setDraftTag(event.target.value);
                  setFeedback(null);
                }}
                onKeyDown={handleTagKeyDown}
                className="text-field community-detail-edit-tag-input"
                placeholder={selectedTags.length >= 7 ? '태그는 최대 7개' : '태그 추가'}
              />

              {draftTag.trim() && selectedTags.length < 7 && suggestedTags.length > 0 ? (
                <div className="community-detail-edit-tag-list community-write-suggested-tag-list">
                  {suggestedTags.map((tag) => (
                    <button
                      key={tag.tag}
                      type="button"
                      className="community-detail-edit-tag"
                      onClick={() => handleAddTag(tag.tag)}
                    >
                      <span>#{tag.tag}</span>
                    </button>
                  ))}
                </div>
              ) : null}
            </div>

            <div className="community-content-body">
              <div className="community-editor-shell community-detail-editor-shell">
                <div className="community-editor-toolbar community-detail-editor-toolbar">
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => runEditorCommand('bold')}
                    aria-label="굵게"
                  >
                    <BoldToolIcon />
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => runEditorCommand('underline')}
                    aria-label="밑줄"
                  >
                    <UnderlineToolIcon />
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => runEditorCommand('formatBlock', 'blockquote')}
                    aria-label="인용"
                  >
                    <QuoteToolIcon />
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={insertCodeBlock}
                    aria-label="코드 영역"
                  >
                    <CodeToolIcon />
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => imageInputRef.current?.click()}
                    aria-label="이미지 첨부"
                  >
                    <ImageToolIcon />
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={insertDisclosureBlock}
                    aria-label="접고 펼치기 영역"
                  >
                    <DisclosureToolIcon />
                  </button>
                </div>

                <div
                  ref={editorRef}
                  className={`community-editor-body community-detail-editor-body ${isEditorEmpty ? 'is-empty' : ''}`.trim()}
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

              <div className="community-write-text-actions">
                <button type="button" className="community-write-text-action is-cancel" onClick={handleCancel}>
                  취소
                </button>
                <button type="button" className="community-write-text-action is-draft" onClick={handleDraftSave}>
                  임시저장
                </button>
                <button
                  type="button"
                  className="community-write-text-action is-submit"
                  onClick={() => void handleSubmit()}
                  disabled={isSubmitting}
                >
                  업로드
                </button>
              </div>
            </div>
          </div>
        </section>

        {feedback ? (
          <section className="panel-card compact community-feedback-card">
            <p className="community-feedback-text">{feedback}</p>
          </section>
        ) : null}
      </>
    );
  }

  if (isLoading) {
    const loadingPanel = (
      <section className="panel-card community-detail-card community-detail-loading-card community-write-page">
        {embedded ? null : (
          <div className="community-detail-topbar">
            <div className="solve-dbms-tab-row community-detail-tab-row" aria-label="커뮤니티 글쓰기 로딩">
              <span className="solve-dbms-tab is-selected community-detail-category-tab">{pageChip}</span>
            </div>
          </div>
        )}

        <div className="community-detail-loading-shell is-loading">
          <div className="community-detail-header community-detail-loading-body" aria-hidden="true">
            <span className="community-loading-placeholder is-long" />
            <div className="community-detail-tags">
              <span className="community-loading-placeholder is-short" />
              <span className="community-loading-placeholder is-short" />
            </div>
            <div className="community-content-body">
              <span className="community-loading-placeholder is-long" />
              <span className="community-loading-placeholder is-long" />
              <span className="community-loading-placeholder is-medium" />
            </div>
          </div>

          <div className="submit-history-loading-overlay" aria-live="polite" aria-label="로딩 중">
            <span className="page-loading-spinner submit-history-loading-badge" aria-hidden="true" />
          </div>
        </div>
      </section>
    );

    return embedded ? (
      <div className="community-detail-page community-write-root community-write-embedded-root">{loadingPanel}</div>
    ) : (
      <div className="page-stack community-detail-page community-write-root">{loadingPanel}</div>
    );
  }

  if (postId && notFound) {
    const notFoundPanel = (
      <section className="panel-card community-detail-card community-write-page">
        {embedded ? null : (
          <div className="community-detail-topbar">
            <div className="solve-dbms-tab-row community-detail-tab-row" aria-label="커뮤니티 글쓰기">
              <span className="solve-dbms-tab is-selected community-detail-category-tab">{pageChip}</span>
            </div>
          </div>
        )}

        <div className="community-detail-header">
          <PageLoadFailureState />
        </div>
      </section>
    );

    return embedded ? (
      <div className="community-detail-page community-write-root community-write-embedded-root">{notFoundPanel}</div>
    ) : (
      <div className="page-stack community-detail-page community-write-root">{notFoundPanel}</div>
    );
  }

  return embedded ? (
    <div className="community-detail-page community-write-root community-write-embedded-root">{renderWritePanel(false)}</div>
  ) : (
    <div className="page-stack community-detail-page community-write-root">{renderWritePanel(true)}</div>
  );
}
