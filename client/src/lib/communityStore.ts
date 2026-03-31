import { mockCommunityPosts, mockCommunitySeedLikedPostIds, getMockCommunityComments } from '../mocks/community';
import { mockCurrentHandle } from '../mocks/profile';
import type { CommunityComment, CommunityPostSummary } from '../types/domain';

const COMMUNITY_STORAGE_KEY = 'quertimizer.community.state';
const COMMUNITY_CHANGE_EVENT = 'quertimizer:community-change';

export interface CommunityEditorDraft {
  title: string;
  draftTag: string;
  selectedTags: string[];
  contentHtml: string;
  updatedAt: string;
}

interface CommunityStoreState {
  customPosts: CommunityPostSummary[];
  commentsByPostId: Record<string, CommunityComment[]>;
  likedPostIds: string[];
  deletedPostIds: string[];
  drafts: Record<string, CommunityEditorDraft>;
}

export interface CommunityActivityCommentItem {
  id: string;
  postId: string;
  postTitle: string;
  content: string;
  createdAt: string;
  depth: number;
}

interface FlattenedCommentItem extends CommunityActivityCommentItem {
  authorHandle: string;
}

const defaultState: CommunityStoreState = {
  customPosts: [],
  commentsByPostId: {},
  likedPostIds: mockCommunitySeedLikedPostIds,
  deletedPostIds: [],
  drafts: {},
};

function emitCommunityChange() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(COMMUNITY_CHANGE_EVENT));
}

function sanitizeState(value: unknown): CommunityStoreState {
  if (!value || typeof value !== 'object') {
    return defaultState;
  }

  const state = value as Partial<CommunityStoreState>;

  return {
    customPosts: Array.isArray(state.customPosts) ? state.customPosts : defaultState.customPosts,
    commentsByPostId: state.commentsByPostId && typeof state.commentsByPostId === 'object' ? state.commentsByPostId : {},
    likedPostIds: Array.isArray(state.likedPostIds) ? state.likedPostIds : defaultState.likedPostIds,
    deletedPostIds: Array.isArray(state.deletedPostIds) ? state.deletedPostIds : defaultState.deletedPostIds,
    drafts: state.drafts && typeof state.drafts === 'object' ? state.drafts : {},
  };
}

function readCommunityState() {
  if (typeof window === 'undefined') {
    return defaultState;
  }

  try {
    const rawValue = window.localStorage.getItem(COMMUNITY_STORAGE_KEY);
    if (!rawValue) {
      return defaultState;
    }

    return sanitizeState(JSON.parse(rawValue));
  } catch {
    return defaultState;
  }
}

function writeCommunityState(nextState: CommunityStoreState) {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(COMMUNITY_STORAGE_KEY, JSON.stringify(nextState));
  emitCommunityChange();
}

function stripHtml(value: string) {
  return value
    .replace(/<img[\s\S]*?>/gi, ' ')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<\/(p|h1|h2|h3|blockquote|li|ul|ol|figure|figcaption)>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function createExcerpt(value: string) {
  const normalized = stripHtml(value);
  return normalized.length > 120 ? `${normalized.slice(0, 120).trim()}...` : normalized;
}

function countComments(comments: CommunityComment[]): number {
  return comments.reduce((total, comment) => total + 1 + countComments(comment.replies), 0);
}

function mergeRawPosts(state: CommunityStoreState) {
  const postById = new Map(mockCommunityPosts.map((post) => [post.id, post]));

  for (const customPost of state.customPosts) {
    postById.set(customPost.id, customPost);
  }

  return Array.from(postById.values()).filter((post) => !state.deletedPostIds.includes(post.id));
}

function buildResolvedPost(post: CommunityPostSummary, state: CommunityStoreState): CommunityPostSummary {
  const comments = state.commentsByPostId[post.id];
  const isLiked = state.likedPostIds.includes(post.id);

  return {
    ...post,
    likes: post.likes + (isLiked ? 1 : 0),
    comments: comments ? countComments(comments) : post.comments,
  };
}

function getRawPostById(postId: string, state = readCommunityState()) {
  return mergeRawPosts(state).find((post) => post.id === postId);
}

function addReplyToTree(comments: CommunityComment[], targetId: string, reply: CommunityComment): CommunityComment[] {
  return comments.map((comment) => {
    if (comment.id === targetId) {
      return {
        ...comment,
        replies: [...comment.replies, reply],
      };
    }

    if (comment.replies.length === 0) {
      return comment;
    }

    return {
      ...comment,
      replies: addReplyToTree(comment.replies, targetId, reply),
    };
  });
}

function flattenComments(
  postId: string,
  postTitle: string,
  comments: CommunityComment[],
  depth = 0
): FlattenedCommentItem[] {
  return comments.flatMap((comment) => [
    {
      id: comment.id,
      postId,
      postTitle,
      content: comment.content,
      createdAt: comment.createdAt,
      depth,
      authorHandle: comment.authorHandle,
    },
    ...flattenComments(postId, postTitle, comment.replies, depth + 1),
  ]);
}

export function subscribeCommunityStore(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(COMMUNITY_CHANGE_EVENT, callback);
  window.addEventListener('storage', callback);

  return () => {
    window.removeEventListener(COMMUNITY_CHANGE_EVENT, callback);
    window.removeEventListener('storage', callback);
  };
}

export function getCommunityStoreSnapshot() {
  if (typeof window === 'undefined') {
    return '';
  }

  return window.localStorage.getItem(COMMUNITY_STORAGE_KEY) ?? '';
}

export function getCommunityPosts() {
  const state = readCommunityState();
  return mergeRawPosts(state).map((post) => buildResolvedPost(post, state));
}

export function getCommunityPostById(postId: string) {
  const state = readCommunityState();
  const post = getRawPostById(postId, state);
  return post ? buildResolvedPost(post, state) : undefined;
}

export function getCommunityComments(postId: string) {
  const state = readCommunityState();
  return state.commentsByPostId[postId] ?? getMockCommunityComments(postId);
}

export function isCommunityPostLiked(postId: string) {
  return readCommunityState().likedPostIds.includes(postId);
}

export function toggleCommunityPostLike(postId: string) {
  const state = readCommunityState();
  const isLiked = state.likedPostIds.includes(postId);
  const likedPostIds = isLiked
    ? state.likedPostIds.filter((currentId) => currentId !== postId)
    : [...state.likedPostIds, postId];

  writeCommunityState({
    ...state,
    likedPostIds,
  });

  return !isLiked;
}

export function saveCommunityPost({
  postId,
  title,
  tags,
  contentHtml,
  authorHandle = mockCurrentHandle,
}: {
  postId?: string;
  title: string;
  tags: string[];
  contentHtml: string;
  authorHandle?: string;
}) {
  const state = readCommunityState();
  const existingPost = postId ? getRawPostById(postId, state) : undefined;
  const content = stripHtml(contentHtml);
  const timestamp = new Date().toISOString();
  const nextPostId = existingPost?.id ?? `community-user-${Date.now()}`;
  const nextComments = state.commentsByPostId[nextPostId] ?? (existingPost ? getCommunityComments(nextPostId) : []);

  const nextPost: CommunityPostSummary = {
    id: nextPostId,
    title: title.trim(),
    authorHandle: existingPost?.authorHandle ?? authorHandle,
    excerpt: createExcerpt(contentHtml),
    content,
    contentHtml,
    tags,
    category: existingPost?.category ?? 'discussion',
    createdAt: existingPost?.createdAt ?? timestamp,
    updatedAt: existingPost ? timestamp : undefined,
    views: existingPost?.views ?? 0,
    likes: existingPost?.likes ?? 0,
    comments: countComments(nextComments),
    isPinned: existingPost?.isPinned ?? false,
    isResolved: existingPost?.isResolved ?? false,
  };

  const customPosts = [...state.customPosts.filter((post) => post.id !== nextPost.id), nextPost];

  writeCommunityState({
    ...state,
    customPosts,
  });

  return nextPost.id;
}

export function deleteCommunityPost(postId: string) {
  const state = readCommunityState();
  const nextDeletedIds = state.deletedPostIds.includes(postId) ? state.deletedPostIds : [...state.deletedPostIds, postId];

  writeCommunityState({
    ...state,
    customPosts: state.customPosts.filter((post) => post.id !== postId),
    likedPostIds: state.likedPostIds.filter((currentId) => currentId !== postId),
    deletedPostIds: nextDeletedIds,
  });
}

export function addCommunityComment({
  postId,
  content,
  authorHandle = mockCurrentHandle,
  parentId,
}: {
  postId: string;
  content: string;
  authorHandle?: string;
  parentId?: string;
}) {
  const state = readCommunityState();
  const currentComments = state.commentsByPostId[postId] ?? getMockCommunityComments(postId);
  const trimmedContent = content.trim();

  if (!trimmedContent) {
    return currentComments;
  }

  const newComment: CommunityComment = {
    id: `community-comment-${Date.now()}`,
    authorHandle,
    content: trimmedContent,
    createdAt: new Date().toISOString(),
    likes: 0,
    replies: [],
  };

  const nextComments = parentId ? addReplyToTree(currentComments, parentId, newComment) : [newComment, ...currentComments];

  writeCommunityState({
    ...state,
    commentsByPostId: {
      ...state.commentsByPostId,
      [postId]: nextComments,
    },
  });

  return nextComments;
}

export function getCommunityEditorDraft(draftKey: string) {
  return readCommunityState().drafts[draftKey];
}

export function saveCommunityEditorDraft(draftKey: string, draft: Omit<CommunityEditorDraft, 'updatedAt'>) {
  const state = readCommunityState();

  writeCommunityState({
    ...state,
    drafts: {
      ...state.drafts,
      [draftKey]: {
        ...draft,
        updatedAt: new Date().toISOString(),
      },
    },
  });
}

export function clearCommunityEditorDraft(draftKey: string) {
  const state = readCommunityState();

  if (!state.drafts[draftKey]) {
    return;
  }

  const { [draftKey]: _, ...restDrafts } = state.drafts;

  writeCommunityState({
    ...state,
    drafts: restDrafts,
  });
}

export function getCommunityActivityData(handle = mockCurrentHandle) {
  const state = readCommunityState();
  const likedPostIds = new Set(state.likedPostIds);
  const posts = mergeRawPosts(state)
    .map((post) => buildResolvedPost(post, state))
    .sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt));
  const authoredPosts = posts.filter((post) => post.authorHandle === handle);
  const likedPosts = posts.filter((post) => likedPostIds.has(post.id));
  const authoredComments = posts
    .flatMap((post) => flattenComments(post.id, post.title, getCommunityComments(post.id)).filter((comment) => comment.authorHandle === handle))
    .sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt));

  return {
    posts: authoredPosts,
    likedPosts,
    comments: authoredComments.map(({ authorHandle: _authorHandle, ...comment }) => comment),
  };
}

