import { EditorContent, ReactNodeViewRenderer, useEditor } from '@tiptap/react';
import type { ChainedCommands } from '@tiptap/core';
import { type ClipboardEvent, type ChangeEvent, type DragEvent, type MouseEvent, type TouchEvent, useEffect, useMemo, useRef, useState } from 'react';
import {
  COMMUNITY_IMAGE_MAX_WIDTH,
  COMMUNITY_IMAGE_MIN_WIDTH,
  createCommunityTiptapExtensions,
  createCommunityEditorSnapshot,
  parseCommunityContentJson,
  type CommunityEditorSnapshot,
  type CommunityUploadedImage,
} from '@/entities/community';
import { useUiText } from '@/shared/config/ui-text';
import { ResizableCommunityImage } from './ResizableCommunityImage';

interface CommunityTiptapEditorProps {
  initialContentJson?: string;
  placeholder?: string;
  editorBodyClassName?: string;
  onSnapshot: (snapshot: CommunityEditorSnapshot) => void;
  onUploadImage: (file: File) => Promise<CommunityUploadedImage>;
  onFeedback?: (message: string | null) => void;
}

interface ToolbarButtonProps {
  icon?: ToolbarIcon;
  label?: string;
  title: string;
  active?: boolean;
  disabled?: boolean;
  onClick: () => void;
}

type ToolbarIcon =
  | 'bold'
  | 'strike'
  | 'underline'
  | 'codeBlock'
  | 'image'
  | 'undo'
  | 'redo';

interface CommunityImageSize {
  width: number;
  height: number;
}

interface CommunityEditorSelection {
  from: number;
  to: number;
}

function clampImageWidth(width: number) {
  return Math.min(COMMUNITY_IMAGE_MAX_WIDTH, Math.max(COMMUNITY_IMAGE_MIN_WIDTH, Math.round(width)));
}

function createCommunityImageSize(width: number, height: number): CommunityImageSize | null {
  if (width <= 0 || height <= 0) {
    return null;
  }

  const normalizedWidth = clampImageWidth(width);
  return {
    width: normalizedWidth,
    height: Math.max(1, Math.round(normalizedWidth / (width / height))),
  };
}

function resolveCommunityImageSize(file: File): Promise<CommunityImageSize | null> {
  if (typeof window === 'undefined') {
    return Promise.resolve(null);
  }

  return new Promise((resolve) => {
    const objectUrl = URL.createObjectURL(file);
    const image = new window.Image();

    image.onload = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(createCommunityImageSize(image.naturalWidth, image.naturalHeight));
    };
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(null);
    };
    image.src = objectUrl;
  });
}

function ToolbarIconView({ icon }: { icon: ToolbarIcon }) {
  switch (icon) {
    case 'bold':
      return (
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M5 3h3.5a2.2 2.2 0 0 1 0 4.4H5V3Z" stroke="currentColor" strokeWidth="1.35" strokeLinejoin="round" />
          <path d="M5 7.4h4a2.35 2.35 0 0 1 0 4.7H5V7.4Z" stroke="currentColor" strokeWidth="1.35" strokeLinejoin="round" />
        </svg>
      );
    case 'strike':
      return (
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M5.2 4.2c.7-.8 1.8-1.2 3-1.2 1.6 0 2.8.7 3.4 1.8M4.7 11.7c.8.8 1.9 1.2 3.4 1.2 2 0 3.5-.9 3.5-2.3 0-1-.7-1.6-2.2-2.1M3 8h10" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" />
        </svg>
      );
    case 'underline':
      return (
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M4.8 3v4.2a3.2 3.2 0 0 0 6.4 0V3M4 13h8" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" />
        </svg>
      );
    case 'codeBlock':
      return (
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <rect x="2.7" y="3.3" width="10.6" height="9.4" rx="1.2" stroke="currentColor" strokeWidth="1.25" />
          <path d="m6.4 6-1.7 2 1.7 2M9.6 6l1.7 2-1.7 2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      );
    case 'image':
      return (
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <rect x="2.2" y="3" width="11.6" height="10" rx="1.2" stroke="currentColor" strokeWidth="1.25" />
          <circle cx="5.5" cy="6.3" r="1.1" fill="currentColor" />
          <path d="m4.1 11 2.7-2.8 2.2 2.2 1.4-1.4L12 11" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      );
    case 'undo':
      return (
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M6.5 4.2 3.6 7.1l2.9 2.9M4 7.1h5.1a3.2 3.2 0 1 1 0 6.4H7.7" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      );
    case 'redo':
      return (
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="m9.5 4.2 2.9 2.9-2.9 2.9M12 7.1H6.9a3.2 3.2 0 1 0 0 6.4h1.4" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      );
  }
}

function ToolbarButton({ icon, label, title, active = false, disabled = false, onClick }: ToolbarButtonProps) {
  function handleMouseDown(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault();
  }

  function handleTouchStart(event: TouchEvent<HTMLButtonElement>) {
    event.preventDefault();

    if (!disabled) {
      onClick();
    }
  }

  return (
    <button
      type="button"
      className={`mini-toggle community-editor-tool ${active ? 'is-active' : ''}`.trim()}
      disabled={disabled}
      data-community-editor-tool={icon ?? label ?? title}
      onMouseDown={handleMouseDown}
      onTouchStart={handleTouchStart}
      onClick={onClick}
      title={title}
      aria-label={title}
    >
      {icon ? <ToolbarIconView icon={icon} /> : <span aria-hidden="true">{label}</span>}
    </button>
  );
}

export default function CommunityTiptapEditor({
  initialContentJson,
  placeholder = '',
  editorBodyClassName = '',
  onSnapshot,
  onUploadImage,
  onFeedback,
}: CommunityTiptapEditorProps) {
  const { text } = useUiText();
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const lastInitialContentJsonRef = useRef(initialContentJson);
  const imageInsertSelectionRef = useRef<CommunityEditorSelection | null>(null);
  const [, refreshToolbarState] = useState(0);
  const editorExtensions = useMemo(
    () => createCommunityTiptapExtensions(ReactNodeViewRenderer(ResizableCommunityImage)),
    [],
  );

  const editor = useEditor({
    extensions: editorExtensions,
    content: parseCommunityContentJson(initialContentJson),
    onCreate: ({ editor: currentEditor }) => onSnapshot(createCommunityEditorSnapshot(currentEditor)),
    onUpdate: ({ editor: currentEditor }) => onSnapshot(createCommunityEditorSnapshot(currentEditor)),
    editorProps: {
      attributes: {
        class: 'community-tiptap-prosemirror',
      },
    },
  });

  function shouldFocusEditorAfterToolbarAction() {
    return typeof window === 'undefined' || !window.matchMedia('(pointer: coarse)').matches;
  }

  function createToolbarCommand(): ChainedCommands | null {
    if (!editor) {
      return null;
    }

    const command = editor.chain();
    return shouldFocusEditorAfterToolbarAction() ? command.focus() : command;
  }

  function runToolbarCommand(command: (chain: ChainedCommands) => ChainedCommands) {
    const toolbarCommand = createToolbarCommand();

    if (toolbarCommand == null) {
      return;
    }

    command(toolbarCommand).run();
    refreshToolbarState((currentValue) => currentValue + 1);
  }

  function rememberImageInsertSelection() {
    if (!editor) {
      return;
    }

    const { from, to } = editor.state.selection;
    imageInsertSelectionRef.current = { from, to };
  }

  function insertImageAtRememberedSelection(content: Record<string, unknown>) {
    if (!editor) {
      return;
    }

    const command = editor.chain().focus();
    const selection = imageInsertSelectionRef.current;
    if (selection != null) {
      command.setTextSelection(selection);
      imageInsertSelectionRef.current = null;
    }

    command.insertContent(content).run();
  }

  useEffect(() => {
    if (!editor || lastInitialContentJsonRef.current === initialContentJson) {
      return;
    }

    lastInitialContentJsonRef.current = initialContentJson;
    editor.commands.setContent(parseCommunityContentJson(initialContentJson), { emitUpdate: true });
  }, [editor, initialContentJson]);

  async function insertImageFile(file: File) {
    if (!editor) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      onFeedback?.(text('COMMUNITY_EDITOR_IMAGE_ONLY_MESSAGE', '이미지 파일만 첨부할 수 있습니다.'));
      return;
    }

    try {
      const imageSize = await resolveCommunityImageSize(file);
      const uploadedImage = await onUploadImage(file);
      insertImageAtRememberedSelection({
        type: 'image',
        attrs: {
          src: uploadedImage.imageUrl,
          alt: file.name || text('COMMUNITY_EDITOR_ATTACHED_IMAGE_ALT', '첨부 이미지'),
          imageId: uploadedImage.imageId,
          ...imageSize,
        },
      });
      onFeedback?.(null);
    } catch (error) {
      onFeedback?.(error instanceof Error ? error.message : text('COMMUNITY_EDITOR_IMAGE_UPLOAD_FAIL_MESSAGE', '이미지 업로드에 실패했습니다.'));
    }
  }

  async function insertImageFiles(files: File[]) {
    const imageFiles = files.filter((file) => file.type.startsWith('image/'));

    if (files.length > 0 && imageFiles.length === 0) {
      onFeedback?.(text('COMMUNITY_EDITOR_IMAGE_ONLY_MESSAGE', '이미지 파일만 첨부할 수 있습니다.'));
      return;
    }

    for (const file of imageFiles) {
      await insertImageFile(file);
    }
  }

  function handleImageFileChange(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);

    if (files.length === 0) {
      return;
    }

    void insertImageFiles(files);
    event.target.value = '';
  }

  function handleEditorPaste(event: ClipboardEvent<HTMLDivElement>) {
    const files = Array.from(event.clipboardData.files);

    if (files.length === 0) {
      return;
    }

    rememberImageInsertSelection();
    event.preventDefault();
    void insertImageFiles(files);
  }

  function handleEditorDrop(event: DragEvent<HTMLDivElement>) {
    const files = Array.from(event.dataTransfer.files);

    if (files.length === 0) {
      return;
    }

    rememberImageInsertSelection();
    event.preventDefault();
    void insertImageFiles(files);
  }

  function openImagePicker() {
    rememberImageInsertSelection();
    imageInputRef.current?.click();
  }

  return (
    <div className="community-editor-shell community-detail-editor-shell">
      <input
        ref={imageInputRef}
        type="file"
        accept="image/*"
        multiple
        className="community-editor-file-input"
        onChange={handleImageFileChange}
      />

      <div className="community-editor-toolbar community-detail-editor-toolbar">
        <div className="community-editor-tool-group" aria-label={text('COMMUNITY_EDITOR_TEXT_FORMAT_GROUP_LABEL', '텍스트 서식')}>
          <ToolbarButton
            icon="bold"
            title={text('COMMUNITY_EDITOR_BOLD_TITLE', '굵게')}
            active={editor?.isActive('bold')}
            onClick={() => runToolbarCommand((chain) => chain.toggleBold())}
          />
          <ToolbarButton
            icon="strike"
            title={text('COMMUNITY_EDITOR_STRIKE_TITLE', '취소선')}
            active={editor?.isActive('strike')}
            onClick={() => runToolbarCommand((chain) => chain.toggleStrike())}
          />
          <ToolbarButton
            icon="underline"
            title={text('COMMUNITY_EDITOR_UNDERLINE_TITLE', '밑줄')}
            active={editor?.isActive('underline')}
            onClick={() => runToolbarCommand((chain) => chain.toggleUnderline())}
          />
          <ToolbarButton
            label={text('COMMUNITY_EDITOR_HIGHLIGHT_LABEL', '형광')}
            title={text('COMMUNITY_EDITOR_HIGHLIGHT_TITLE', '하이라이트')}
            active={editor?.isActive('highlight')}
            onClick={() => runToolbarCommand((chain) => chain.toggleHighlight())}
          />
        </div>

        <div className="community-editor-tool-group" aria-label={text('COMMUNITY_EDITOR_CODE_GROUP_LABEL', '코드')}>
          <ToolbarButton
            icon="codeBlock"
            title={text('COMMUNITY_EDITOR_CODE_BLOCK_TITLE', '코드블럭')}
            active={editor?.isActive('codeBlock')}
            onClick={() => runToolbarCommand((chain) => chain.toggleCodeBlock())}
          />
        </div>

        <div className="community-editor-tool-group" aria-label={text('COMMUNITY_EDITOR_BLOCK_GROUP_LABEL', '블록')}>
          <ToolbarButton
            label={text('COMMUNITY_EDITOR_HEADING_LABEL', '제목')}
            title={text('COMMUNITY_EDITOR_HEADING_LABEL', '제목')}
            active={editor?.isActive('heading', { level: 2 })}
            onClick={() => runToolbarCommand((chain) => chain.toggleHeading({ level: 2 }))}
          />
        </div>

        <div className="community-editor-tool-group" aria-label={text('COMMUNITY_EDITOR_LIST_GROUP_LABEL', '목록')}>
          <ToolbarButton
            label={text('COMMUNITY_EDITOR_BULLET_LIST_LABEL', '글머리')}
            title={text('COMMUNITY_EDITOR_BULLET_LIST_TITLE', '글머리 목록')}
            active={editor?.isActive('bulletList')}
            onClick={() => runToolbarCommand((chain) => chain.toggleBulletList())}
          />
          <ToolbarButton
            label={text('COMMUNITY_EDITOR_ORDERED_LIST_LABEL', '번호')}
            title={text('COMMUNITY_EDITOR_ORDERED_LIST_TITLE', '번호 목록')}
            active={editor?.isActive('orderedList')}
            onClick={() => runToolbarCommand((chain) => chain.toggleOrderedList())}
          />
        </div>

        <div className="community-editor-tool-group" aria-label={text('COMMUNITY_EDITOR_INSERT_GROUP_LABEL', '삽입')}>
          <ToolbarButton
            label={text('COMMUNITY_EDITOR_HARD_BREAK_LABEL', '줄바꿈')}
            title={text('COMMUNITY_EDITOR_HARD_BREAK_LABEL', '줄바꿈')}
            onClick={() => runToolbarCommand((chain) => chain.setHardBreak())}
          />
          <ToolbarButton
            icon="image"
            title={text('COMMON_IMAGE_LABEL', '이미지')}
            onClick={openImagePicker}
          />
        </div>

        <div className="community-editor-tool-group" aria-label={text('COMMUNITY_EDITOR_HISTORY_GROUP_LABEL', '히스토리')}>
          <ToolbarButton icon="undo" title={text('COMMUNITY_EDITOR_UNDO_TITLE', '실행취소')} onClick={() => runToolbarCommand((chain) => chain.undo())} />
          <ToolbarButton icon="redo" title={text('COMMUNITY_EDITOR_REDO_TITLE', '다시실행')} onClick={() => runToolbarCommand((chain) => chain.redo())} />
        </div>
      </div>

      <EditorContent
        editor={editor}
        className={`community-editor-body community-detail-editor-body ${editorBodyClassName} ${editor?.isEmpty ? 'is-empty' : ''}`.trim()}
        data-placeholder={placeholder}
        onPaste={handleEditorPaste}
        onDrop={handleEditorDrop}
      />
    </div>
  );
}
