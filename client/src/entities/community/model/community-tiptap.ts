import { Node, mergeAttributes, type Editor, type JSONContent } from '@tiptap/core';
import Image from '@tiptap/extension-image';
import { CodeBlockLowlight } from '@tiptap/extension-code-block-lowlight';
import Highlight from '@tiptap/extension-highlight';
import TaskItem from '@tiptap/extension-task-item';
import TaskList from '@tiptap/extension-task-list';
import Underline from '@tiptap/extension-underline';
import StarterKit from '@tiptap/starter-kit';
import { common, createLowlight } from 'lowlight';
import { COMMUNITY_DISCLOSURE_LABEL } from '@/entities/community';

export interface CommunityEditorSnapshot {
  contentJson: string;
  plainTextSummary: string;
  imageIds: string[];
  contentByteLength: number;
  empty: boolean;
}

export interface CommunityUploadedImage {
  imageId: string;
  imageUrl: string;
}

const lowlight = createLowlight(common);
const utf8Encoder = new TextEncoder();

export const COMMUNITY_EMPTY_DOC: JSONContent = {
  type: 'doc',
  content: [
    {
      type: 'paragraph',
    },
  ],
};

const Details = Node.create({
  name: 'details',
  group: 'block',
  content: 'detailsSummary detailsContent',
  defining: true,

  addAttributes() {
    return {
      open: {
        default: true,
        parseHTML: (element) => (element as HTMLDetailsElement).open,
        renderHTML: (attributes) => (attributes.open ? { open: '' } : {}),
      },
    };
  },

  parseHTML() {
    return [
      {
        tag: 'details',
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return ['details', mergeAttributes(HTMLAttributes, { class: 'community-editor-disclosure' }), 0];
  },
});

const DetailsSummary = Node.create({
  name: 'detailsSummary',
  content: 'inline*',
  defining: true,

  parseHTML() {
    return [
      {
        tag: 'summary',
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return ['summary', mergeAttributes(HTMLAttributes), 0];
  },
});

const DetailsContent = Node.create({
  name: 'detailsContent',
  content: 'block+',
  defining: true,

  parseHTML() {
    return [
      {
        tag: 'div[data-type="details-content"]',
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'details-content' }), 0];
  },
});

const CommunityImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      imageId: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-image-id'),
        renderHTML: (attributes) => (attributes.imageId ? { 'data-image-id': attributes.imageId } : {}),
      },
      class: {
        default: 'community-content-image',
        parseHTML: (element) => element.getAttribute('class') ?? 'community-content-image',
        renderHTML: () => ({ class: 'community-content-image' }),
      },
    };
  },
});

export const communityTiptapExtensions = [
  StarterKit.configure({
    codeBlock: false,
    link: {
      autolink: true,
      linkOnPaste: true,
      openOnClick: false,
    },
    underline: false,
  }),
  Underline,
  Highlight.configure({
    multicolor: false,
  }),
  CodeBlockLowlight.configure({
    lowlight,
    defaultLanguage: 'sql',
    HTMLAttributes: {
      class: 'community-code-block',
    },
  }),
  CommunityImage.configure({
    allowBase64: false,
    inline: false,
  }),
  TaskList.configure({
    HTMLAttributes: {
      class: 'community-task-list',
    },
  }),
  TaskItem.configure({
    nested: true,
    HTMLAttributes: {
      class: 'community-task-item',
    },
  }),
  Details,
  DetailsSummary,
  DetailsContent,
];

export function parseCommunityContentJson(contentJson?: string | null): JSONContent {
  if (!contentJson?.trim()) {
    return COMMUNITY_EMPTY_DOC;
  }

  try {
    const parsedContent = JSON.parse(contentJson) as JSONContent;
    return parsedContent.type === 'doc' ? parsedContent : COMMUNITY_EMPTY_DOC;
  } catch {
    return COMMUNITY_EMPTY_DOC;
  }
}

export function stringifyCommunityContentJson(content: JSONContent) {
  return JSON.stringify(content);
}

export function createCommunityDetailsBlock(): JSONContent {
  return {
    type: 'details',
    attrs: {
      open: true,
    },
    content: [
      {
        type: 'detailsSummary',
        content: [
          {
            type: 'text',
            text: COMMUNITY_DISCLOSURE_LABEL,
          },
        ],
      },
      {
        type: 'detailsContent',
        content: [
          {
            type: 'paragraph',
          },
        ],
      },
    ],
  };
}

export function createCommunityEditorSnapshot(editor: Editor): CommunityEditorSnapshot {
  return createCommunityEditorSnapshotFromContent(editor.getJSON());
}

export function createCommunityEditorSnapshotFromJson(contentJson?: string | null): CommunityEditorSnapshot {
  return createCommunityEditorSnapshotFromContent(parseCommunityContentJson(contentJson));
}

function createCommunityEditorSnapshotFromContent(content: JSONContent): CommunityEditorSnapshot {
  const contentJson = stringifyCommunityContentJson(content);
  const plainTextSummary = createPlainTextSummary(content);

  return {
    contentJson,
    plainTextSummary,
    imageIds: extractCommunityImageIds(content),
    contentByteLength: utf8Encoder.encode(contentJson).length,
    empty: !hasMeaningfulCommunityContent(content),
  };
}

function createPlainTextSummary(content: JSONContent) {
  const plainText = extractPlainText(content)
    .replace(/\s+/g, ' ')
    .trim();

  return plainText.length > 2000 ? plainText.slice(0, 2000).trim() : plainText;
}

function hasMeaningfulCommunityContent(content: JSONContent) {
  return extractPlainText(content).trim() !== '' || extractCommunityImageIds(content).length > 0;
}

function extractPlainText(node: JSONContent, parentType?: string): string {
  if (node.type === 'image') {
    return '이미지';
  }

  if (typeof node.text === 'string') {
    return parentType === 'detailsSummary' ? '' : node.text;
  }

  if (!Array.isArray(node.content)) {
    return '';
  }

  return node.content.map((childNode) => extractPlainText(childNode, node.type)).join(' ');
}

function extractCommunityImageIds(content: JSONContent) {
  const imageIds = new Set<string>();
  collectCommunityImageIds(content, imageIds);
  return Array.from(imageIds);
}

function collectCommunityImageIds(node: JSONContent, imageIds: Set<string>) {
  if (node.type === 'image' && typeof node.attrs?.imageId === 'string') {
    imageIds.add(node.attrs.imageId);
  }

  node.content?.forEach((childNode) => collectCommunityImageIds(childNode, imageIds));
}
