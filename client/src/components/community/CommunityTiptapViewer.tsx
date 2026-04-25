import { EditorContent, useEditor } from '@tiptap/react';
import { useEffect, useRef } from 'react';
import { communityTiptapExtensions, parseCommunityContentJson } from '../../lib/communityTiptap';

interface CommunityTiptapViewerProps {
  contentJson?: string;
}

export default function CommunityTiptapViewer({ contentJson }: CommunityTiptapViewerProps) {
  const lastContentJsonRef = useRef(contentJson);
  const editor = useEditor({
    extensions: communityTiptapExtensions,
    content: parseCommunityContentJson(contentJson),
    editable: false,
    editorProps: {
      attributes: {
        class: 'community-tiptap-prosemirror',
      },
    },
  });

  useEffect(() => {
    if (!editor || lastContentJsonRef.current === contentJson) {
      return;
    }

    lastContentJsonRef.current = contentJson;
    editor.commands.setContent(parseCommunityContentJson(contentJson), { emitUpdate: false });
  }, [editor, contentJson]);

  return (
    <div className="community-detail-rich-content community-tiptap-viewer">
      <EditorContent editor={editor} />
    </div>
  );
}
