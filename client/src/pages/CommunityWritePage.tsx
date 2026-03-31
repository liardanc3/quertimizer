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
  getCommunityPostById,
  saveCommunityEditorDraft,
  saveCommunityPost,
} from '../lib/communityStore';
import { COMMUNITY_PATH, getCommunityPostPath, navigate } from '../lib/navigation';
import { mockCommunityTagLibrary } from '../mocks/community';

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

function getTagScore(label: string, aliases: string[], query: string) {
  if (!query) {
    return 0;
  }

  const candidates = [label, ...aliases].map((candidate) => normalizeKeyword(candidate));
  let score = 0;

  for (const candidate of candidates) {
    if (candidate === query) {
      score = Math.max(score, 4);
      continue;
    }

    if (candidate.startsWith(query)) {
      score = Math.max(score, 3);
      continue;
    }

    if (candidate.includes(query)) {
      score = Math.max(score, 2);
    }
  }

  return score;
}

function resolveTagLabel(value: string) {
  const trimmedValue = value.trim();

  if (!trimmedValue) {
    return '';
  }

  const normalizedValue = normalizeKeyword(trimmedValue);
  const matchedTag = mockCommunityTagLibrary.find((tag) =>
    [tag.label, ...tag.aliases].some((candidate) => normalizeKeyword(candidate) === normalizedValue)
  );

  return matchedTag?.label ?? trimmedValue;
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
  const post = postId ? getCommunityPostById(postId) : undefined;
  const draftKey = postId ? `community-edit-${postId}` : 'community-write';
  const [title, setTitle] = useState('');
  const [draftTag, setDraftTag] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [editorHtml, setEditorHtml] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [draftRecoveredAt, setDraftRecoveredAt] = useState<string | null>(null);

  const editorRef = useRef<HTMLDivElement | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const savedRangeRef = useRef<Range | null>(null);
  const hydratedRef = useRef(false);

  const normalizedDraftTag = normalizeKeyword(draftTag);
  const suggestedTags = useMemo(
    () =>
      mockCommunityTagLibrary
        .filter(
          (tag) =>
            !selectedTags.some((selectedTag) => normalizeKeyword(selectedTag) === normalizeKeyword(tag.label))
        )
        .map((tag) => ({
          ...tag,
          score: getTagScore(tag.label, tag.aliases, normalizedDraftTag),
        }))
        .filter((tag) => normalizedDraftTag && tag.score > 0)
        .sort((left, right) => right.score - left.score || right.usageCount - left.usageCount)
        .slice(0, 5),
    [normalizedDraftTag, selectedTags]
  );
  const isEditorEmpty = !hasMeaningfulHtml(editorHtml);

  useEffect(() => {
    if (postId && !post) {
      return;
    }

    const savedDraft = getCommunityEditorDraft(draftKey);
    const baseValues: EditorValues = post
      ? {
          title: post.title,
          draftTag: '',
          selectedTags: post.tags,
          contentHtml: post.contentHtml ?? `<p>${post.content}</p>`,
        }
      : createEmptyValues();

    const nextValues: EditorValues = savedDraft
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

    window.requestAnimationFrame(() => {
      if (editorRef.current) {
        editorRef.current.innerHTML = nextValues.contentHtml;
      }
    });
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

  if (postId && !post) {
    return (
      <div className="page-stack">
        <section className="panel-card community-detail-card">
          <button type="button" className="btn ghost community-back-button" onClick={() => navigate(COMMUNITY_PATH)}>
            뒤로가기
          </button>
          <p className="panel-meta">커뮤니티</p>
          <h1 className="page-title">수정할 게시글을 찾을 수 없습니다.</h1>
          <p className="muted-text">삭제되었거나 잘못된 경로입니다. 목록으로 돌아가 다시 확인해 주세요.</p>
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
      `<figure class="community-editor-figure"><img src="${escapeHtmlAttribute(source)}" alt="${escapeHtmlAttribute(altText)}" /></figure><p><br /></p>`
    );
    syncEditorHtml();
  }

  function readAndInsertImage(file: File) {
    if (!file.type.startsWith('image/')) {
      setFeedback('이미지 파일만 첨부할 수 있습니다.');
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
    const resolvedLabel = resolveTagLabel(tagLabel);
    const normalizedLabel = normalizeKeyword(resolvedLabel);

    if (!normalizedLabel) {
      return;
    }

    setSelectedTags((currentTags) =>
      currentTags.some((currentTag) => normalizeKeyword(currentTag) === normalizedLabel)
        ? currentTags
        : [...currentTags, resolvedLabel]
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
    const baseValues: EditorValues = post
      ? {
          title: post.title,
          draftTag: '',
          selectedTags: post.tags,
          contentHtml: post.contentHtml ?? `<p>${post.content}</p>`,
        }
      : createEmptyValues();

    setTitle(baseValues.title);
    setDraftTag(baseValues.draftTag);
    setSelectedTags(baseValues.selectedTags);
    setEditorHtml(baseValues.contentHtml);
    clearCommunityEditorDraft(draftKey);
    setDraftRecoveredAt(null);

    if (editorRef.current) {
      editorRef.current.innerHTML = baseValues.contentHtml;
    }
  }

  function handleClearDraft() {
    clearCommunityEditorDraft(draftKey);
    setDraftRecoveredAt(null);
    setFeedback('임시저장을 비웠습니다.');
  }

  function handleSubmit() {
    if (!title.trim() || !hasMeaningfulHtml(editorHtml)) {
      setFeedback('제목과 본문은 반드시 입력해야 합니다.');
      return;
    }

    const savedPostId = saveCommunityPost({
      postId,
      title,
      tags: selectedTags,
      contentHtml: editorHtml,
    });

    clearCommunityEditorDraft(draftKey);
    navigate(getCommunityPostPath(savedPostId), {
      state: {
        from: window.history.state?.from ?? COMMUNITY_PATH,
      },
    });
  }

  const savedDraftLabel = draftRecoveredAt ? new Date(draftRecoveredAt).toLocaleString('ko-KR') : null;
  const pageTitle = postId ? '게시글 수정' : '글쓰기';
  const pageMeta = '커뮤니티';
  const pageChip = postId ? '글 수정' : '글쓰기';

  return (
    <div className="page-stack">
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
          <p className="panel-meta">{pageMeta}</p>
          <h1 className="page-title">{pageTitle}</h1>
          <p className="muted-text">태그 추천, 이미지 첨부, 본문 서식, 임시저장 복구까지 한 화면에서 이어집니다.</p>
        </div>

        {savedDraftLabel ? (
          <div className="community-draft-strip">
            <div className="community-draft-copy">
              <strong>임시저장을 불러왔습니다.</strong>
              <span>{savedDraftLabel} 기준으로 이어서 작성 중입니다.</span>
            </div>
            <div className="community-draft-actions">
              <button type="button" className="btn ghost" onClick={handleClearDraft}>
                임시저장 비우기
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
                placeholder="제목을 입력하세요."
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
                    placeholder="예: left_join, 101"
                  />
                  <button
                    type="button"
                    className="btn secondary community-tag-submit-button"
                    onClick={handleAddDraftTag}
                    disabled={!draftTag.trim()}
                    aria-label="입력한 태그 추가"
                    title="입력한 태그 추가"
                  >
                    <span aria-hidden="true">↵</span>
                  </button>
                </div>

                {draftTag.trim() ? (
                  <div className="community-tag-related-box">
                    <div className="community-tag-related-head">
                      <span className="community-tag-related-title">관련 태그</span>
                      <span className="community-tag-related-caption">입력할수록 더 가깝게 맞춰집니다.</span>
                    </div>

                    {suggestedTags.length > 0 ? (
                      <div className="community-tag-related-list">
                        {suggestedTags.map((tag) => (
                          <button
                            key={tag.id}
                            type="button"
                            className="community-tag-related-item"
                            onClick={() => handleAddTag(tag.label)}
                          >
                            <span className="community-tag-related-name">#{tag.label}</span>
                            <span className="community-tag-related-desc">{tag.description}</span>
                          </button>
                        ))}
                      </div>
                    ) : (
                      <p className="community-tag-related-empty">비슷한 태그가 없으면 새 태그로 바로 추가됩니다.</p>
                    )}
                  </div>
                ) : (
                  <p className="hint-text community-tag-helper">
                    태그를 한 글자씩 입력할 때마다 비슷한 태그가 아래에 바로 나타납니다.
                  </p>
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
                <span className="hint-text">굵게, 소제목, 목록, 인용, 이미지 첨부</span>
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
                    onClick={() => runEditorCommand('formatBlock', 'h2')}
                  >
                    H2
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => runEditorCommand('insertUnorderedList')}
                  >
                    목록
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => runEditorCommand('formatBlock', 'blockquote')}
                  >
                    인용
                  </button>
                  <button
                    type="button"
                    className="mini-toggle community-editor-tool"
                    onMouseDown={handleToolbarMouseDown}
                    onClick={() => {
                      rememberSelection();
                      imageInputRef.current?.click();
                    }}
                  >
                    이미지
                  </button>
                </div>

                <div
                  ref={editorRef}
                  className={`community-editor-surface${isEditorEmpty ? ' is-empty' : ''}`}
                  contentEditable
                  suppressContentEditableWarning
                  data-placeholder="질문 상황, 시도한 SQL, 실행 계획 이미지 등을 함께 적어주세요."
                  onInput={syncEditorHtml}
                  onPaste={handleEditorPaste}
                  onBlur={rememberSelection}
                  onKeyUp={rememberSelection}
                  onMouseUp={rememberSelection}
                  onFocus={rememberSelection}
                />
              </div>
            </div>

            {feedback ? <p className="community-write-feedback">{feedback}</p> : null}

            <div className="auth-actions">
              <button type="button" className="btn primary" onClick={handleSubmit}>
                {postId ? '수정 저장' : '등록'}
              </button>
              <button type="button" className="btn ghost" onClick={handleCancel}>
                취소
              </button>
            </div>
          </div>

          <aside className="community-write-sidebar">
            <section className="community-sidebar-card">
              <div className="community-sidebar-header">
                <div>
                  <p className="panel-meta">편집 도움</p>
                  <h2 className="panel-title">편집 도움</h2>
                </div>
                <span className="subtle-chip">에디터</span>
              </div>

              <div className="community-write-guide">
                <p>굵게 버튼으로 오류 메시지나 핵심 조건을 바로 강조할 수 있습니다.</p>
                <p>이미지 버튼이나 이미지 붙여넣기로 실행 계획 캡처를 본문 중간에 넣을 수 있습니다.</p>
                <p>임시저장은 자동으로 저장되며, 다음에 다시 와도 이어서 수정할 수 있습니다.</p>
              </div>
            </section>

            <section className="community-sidebar-card">
              <div className="community-sidebar-header">
                <div>
                  <p className="panel-meta">작성 안내</p>
                  <h2 className="panel-title">작성 팁</h2>
                </div>
                <span className="subtle-chip">가이드</span>
              </div>

              <div className="community-write-guide">
                <p>제목에는 문제 번호나 핵심 키워드를 같이 넣으면 검색 노출에 유리합니다.</p>
                <p>질문 글이라면 기대한 결과와 실제 결과, 시도한 SQL을 함께 적어두는 편이 좋습니다.</p>
                <p>태그는 입력 중 바로 뜨는 관련 태그를 먼저 눌러서 표기 중복을 줄이도록 구성했습니다.</p>
              </div>
            </section>
          </aside>
        </div>
      </section>
    </div>
  );
}
