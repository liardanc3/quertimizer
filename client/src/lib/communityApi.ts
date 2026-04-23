import { getApiBaseUrl } from './authApi';
import type { CommunityComment, CommunityPostSummary } from '../types/domain';

interface CommunityCommentResponse {
  commentId?: number;
  authorId?: string;
  content?: string;
  createdAt?: string;
  likeCount?: number;
  likedByCurrentUser?: boolean;
  replies?: CommunityCommentResponse[];
}

interface CommunityPostSummaryResponse {
  postId?: string;
  title?: string;
  authorId?: string;
  excerpt?: string;
  tags?: string[];
  category?: string;
  createdAt?: string;
  updatedAt?: string | null;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
}

interface CommunityPostPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  posts?: CommunityPostSummaryResponse[];
}

interface CommunityPostDetailResponse extends CommunityPostSummaryResponse {
  contentHtml?: string;
  likedByCurrentUser?: boolean;
  editable?: boolean;
  comments?: CommunityCommentResponse[];
}

interface CommunityReactionResponse {
  liked?: boolean;
  likeCount?: number;
}

interface CommunityTagSuggestionResponse {
  tag?: string;
  usageCount?: number;
}

interface UserProfileCommunityPostResponse {
  postId?: string;
  title?: string;
  excerpt?: string;
  tags?: string[];
  createdAt?: string;
  updatedAt?: string | null;
  likeCount?: number;
  commentCount?: number;
}

interface UserProfileCommunityPostsResponse {
  posts?: UserProfileCommunityPostResponse[];
}

interface UserProfileCommunityCommentResponse {
  commentId?: number;
  postId?: string;
  postTitle?: string;
  content?: string;
  createdAt?: string;
  reply?: boolean;
}

interface UserProfileCommunityCommentsResponse {
  comments?: UserProfileCommunityCommentResponse[];
}

export interface FetchCommunityPostsParams {
  page: number;
  search: string;
  tag: string;
  category: 'all' | 'discussion' | 'question' | 'notice';
  sortKey: 'default' | 'latest' | 'oldest' | 'views' | 'viewsAsc' | 'likes' | 'likesAsc' | 'comments' | 'commentsAsc';
}

export interface CommunityPostPage {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  posts: CommunityPostSummary[];
}

export interface CommunityPostDetail {
  postId: string;
  title: string;
  authorHandle: string;
  excerpt: string;
  content: string;
  contentHtml: string;
  tags: string[];
  category: CommunityPostSummary['category'];
  createdAt: string;
  updatedAt?: string;
  views: number;
  likes: number;
  comments: number;
  likedByCurrentUser: boolean;
  editable: boolean;
  commentTree: CommunityComment[];
}

export interface CommunityReaction {
  liked: boolean;
  likeCount: number;
}

export interface CommunityTagSuggestion {
  tag: string;
  usageCount: number;
}

export interface SaveCommunityPostPayload {
  title: string;
  tags: string[];
  contentHtml: string;
}

export interface AddCommunityCommentPayload {
  content: string;
  parentCommentId?: number;
}

export interface ProfileCommunityPost {
  postId: string;
  title: string;
  excerpt: string;
  tags: string[];
  createdAt: string;
  updatedAt?: string;
  likeCount: number;
  commentCount: number;
}

export interface ProfileCommunityComment {
  commentId: number;
  postId: string;
  postTitle: string;
  content: string;
  createdAt: string;
  reply: boolean;
}

function normalizeComment(comment: CommunityCommentResponse): CommunityComment {
  return {
    id: String(comment.commentId!),
    authorHandle: comment.authorId!,
    content: comment.content!,
    createdAt: comment.createdAt!,
    likes: comment.likeCount!,
    likedByCurrentUser: comment.likedByCurrentUser === true,
    replies: Array.isArray(comment.replies) ? comment.replies.map(normalizeComment) : [],
  };
}

function normalizePostCategory(value?: string): CommunityPostSummary['category'] {
  if (value === 'question' || value === 'notice' || value === 'tip') {
    return value;
  }

  return 'discussion';
}

function normalizeTags(tags?: string[]) {
  if (!Array.isArray(tags)) {
    return [];
  }

  const uniqueTags = new Set<string>();

  tags.forEach((tag) => {
    if (typeof tag !== 'string') {
      return;
    }

    const normalizedTag = tag.trim();

    if (normalizedTag !== '') {
      uniqueTags.add(normalizedTag);
    }
  });

  return Array.from(uniqueTags);
}

function normalizePostSummary(post: CommunityPostSummaryResponse): CommunityPostSummary {
  return {
    id: post.postId!,
    title: post.title!,
    authorHandle: post.authorId!,
    excerpt: post.excerpt ?? '',
    content: '',
    contentHtml: undefined,
    tags: normalizeTags(post.tags),
    category: normalizePostCategory(post.category),
    createdAt: post.createdAt!,
    updatedAt: typeof post.updatedAt === 'string' ? post.updatedAt : undefined,
    views: post.viewCount ?? 0,
    likes: post.likeCount ?? 0,
    comments: post.commentCount ?? 0,
    isPinned: false,
    isResolved: false,
  };
}

function extractPlainText(value: string) {
  return value
    .replace(/<img[\s\S]*?>/gi, ' ')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<\/(p|div|li|h1|h2|h3|blockquote|figure|figcaption)>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

async function requestCommunity<T>(path: string, init: RequestInit, fallbackMessage: string, normalize: (data: unknown) => T): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      credentials: 'include',
      ...init,
    });
  } catch {
    throw new Error(fallbackMessage);
  }

  if (!response.ok) {
    throw new Error(fallbackMessage);
  }

  try {
    const data = (await response.json()) as unknown;
    return normalize(data);
  } catch {
    throw new Error(fallbackMessage);
  }
}

export async function fetchCommunityPosts(params: FetchCommunityPostsParams): Promise<CommunityPostPage> {
  const searchParams = new URLSearchParams({
    page: String(params.page),
    sortKey: params.sortKey,
  });

  if (params.search.trim() !== '') {
    searchParams.set('search', params.search.trim());
  }

  if (params.tag.trim() !== '') {
    searchParams.set('tag', params.tag.trim());
  }

  if (params.category !== 'all') {
    searchParams.set('category', params.category);
  }

  return requestCommunity(
    `/community/posts?${searchParams.toString()}`,
    { method: 'GET' },
    '커뮤니티 게시글 조회에 실패했다.',
    (data) => {
      const page = data as CommunityPostPageResponse;
      if (
        typeof page.currentPage !== 'number' ||
        typeof page.pageSize !== 'number' ||
        typeof page.totalCount !== 'number' ||
        typeof page.totalPages !== 'number' ||
        !Array.isArray(page.posts)
      ) {
        throw new Error();
      }

      return {
        currentPage: page.currentPage,
        pageSize: page.pageSize,
        totalCount: page.totalCount,
        totalPages: page.totalPages,
        posts: page.posts
          .filter(
            (post): post is Required<Pick<CommunityPostSummaryResponse, 'postId' | 'title' | 'authorId' | 'createdAt'>> & CommunityPostSummaryResponse =>
              typeof post.postId === 'string' &&
              typeof post.title === 'string' &&
              typeof post.authorId === 'string' &&
              typeof post.createdAt === 'string',
          )
          .map(normalizePostSummary),
      } satisfies CommunityPostPage;
    },
  );
}

export async function fetchCommunityPostDetail(postId: string): Promise<CommunityPostDetail> {
  return requestCommunity(
    `/community/posts/${encodeURIComponent(postId)}`,
    { method: 'GET' },
    '커뮤니티 게시글 상세 조회에 실패했다.',
    (data) => {
      const post = data as CommunityPostDetailResponse;
      if (
        typeof post.postId !== 'string' ||
        typeof post.title !== 'string' ||
        typeof post.authorId !== 'string' ||
        typeof post.createdAt !== 'string' ||
        typeof post.contentHtml !== 'string' ||
        !Array.isArray(post.comments)
      ) {
        throw new Error();
      }

      return {
        postId: post.postId,
        title: post.title,
        authorHandle: post.authorId,
        excerpt: post.excerpt ?? '',
        content: extractPlainText(post.contentHtml),
        contentHtml: post.contentHtml,
        tags: normalizeTags(post.tags),
        category: normalizePostCategory(post.category),
        createdAt: post.createdAt,
        updatedAt: typeof post.updatedAt === 'string' ? post.updatedAt : undefined,
        views: post.viewCount ?? 0,
        likes: post.likeCount ?? 0,
        comments: post.commentCount ?? 0,
        likedByCurrentUser: post.likedByCurrentUser === true,
        editable: post.editable === true,
        commentTree: post.comments
          .filter(
            (comment): comment is Required<Pick<CommunityCommentResponse, 'commentId' | 'authorId' | 'content' | 'createdAt' | 'likeCount'>> & CommunityCommentResponse =>
              typeof comment.commentId === 'number' &&
              typeof comment.authorId === 'string' &&
              typeof comment.content === 'string' &&
              typeof comment.createdAt === 'string' &&
              typeof comment.likeCount === 'number',
          )
          .map(normalizeComment),
      } satisfies CommunityPostDetail;
    },
  );
}

export async function createCommunityPost(payload: SaveCommunityPostPayload) {
  const response = await fetch(`${getApiBaseUrl()}/community/posts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error('게시글 저장에 실패했다.');
  }

  const location = response.headers.get('Location');
  return location?.split('/').pop() ?? '';
}

export async function updateCommunityPost(postId: string, payload: SaveCommunityPostPayload) {
  const response = await fetch(`${getApiBaseUrl()}/community/posts/${encodeURIComponent(postId)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error('게시글 수정에 실패했다.');
  }
}

export async function deleteCommunityPost(postId: string) {
  const response = await fetch(`${getApiBaseUrl()}/community/posts/${encodeURIComponent(postId)}`, {
    method: 'DELETE',
    credentials: 'include',
  });

  if (!response.ok) {
    throw new Error('게시글 삭제에 실패했다.');
  }
}

export async function toggleCommunityPostLike(postId: string): Promise<CommunityReaction> {
  return requestCommunity(
    `/community/posts/${encodeURIComponent(postId)}/likes`,
    { method: 'POST' },
    '게시글 좋아요 처리에 실패했다.',
    (data) => {
      const reaction = data as CommunityReactionResponse;
      if (typeof reaction.liked !== 'boolean' || typeof reaction.likeCount !== 'number') {
        throw new Error();
      }

      return {
        liked: reaction.liked,
        likeCount: reaction.likeCount,
      };
    },
  );
}

export async function addCommunityComment(postId: string, payload: AddCommunityCommentPayload): Promise<CommunityComment> {
  return requestCommunity(
    `/community/posts/${encodeURIComponent(postId)}/comments`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
    '댓글 등록에 실패했다.',
    (data) => {
      const comment = data as CommunityCommentResponse;
      if (
        typeof comment.commentId !== 'number' ||
        typeof comment.authorId !== 'string' ||
        typeof comment.content !== 'string' ||
        typeof comment.createdAt !== 'string' ||
        typeof comment.likeCount !== 'number'
      ) {
        throw new Error();
      }

      return normalizeComment(comment);
    },
  );
}

export async function toggleCommunityCommentLike(commentId: string): Promise<CommunityReaction> {
  return requestCommunity(
    `/community/comments/${encodeURIComponent(commentId)}/likes`,
    { method: 'POST' },
    '댓글 좋아요 처리에 실패했다.',
    (data) => {
      const reaction = data as CommunityReactionResponse;
      if (typeof reaction.liked !== 'boolean' || typeof reaction.likeCount !== 'number') {
        throw new Error();
      }

      return {
        liked: reaction.liked,
        likeCount: reaction.likeCount,
      };
    },
  );
}

export async function fetchCommunityTagSuggestions(query: string): Promise<CommunityTagSuggestion[]> {
  const searchParams = new URLSearchParams();
  if (query.trim() !== '') {
    searchParams.set('query', query.trim());
  }

  return requestCommunity(
    `/community/tags/suggestions?${searchParams.toString()}`,
    { method: 'GET' },
    '태그 자동완성 조회에 실패했다.',
    (data) => {
      if (!Array.isArray(data)) {
        throw new Error();
      }

      return data
        .filter(
          (tag): tag is Required<Pick<CommunityTagSuggestionResponse, 'tag' | 'usageCount'>> & CommunityTagSuggestionResponse =>
            typeof (tag as CommunityTagSuggestionResponse).tag === 'string' &&
            typeof (tag as CommunityTagSuggestionResponse).usageCount === 'number',
        )
        .map((tag) => ({
          tag: tag.tag,
          usageCount: tag.usageCount,
        }));
    },
  );
}

async function fetchProfileCommunityPosts(path: string): Promise<ProfileCommunityPost[]> {
  return requestCommunity(
    path,
    { method: 'GET' },
    '프로필 커뮤니티 게시글 조회에 실패했다.',
    (data) => {
      const response = data as UserProfileCommunityPostsResponse;
      if (!Array.isArray(response.posts)) {
        throw new Error();
      }

      return response.posts
        .filter(
          (post): post is Required<Pick<UserProfileCommunityPostResponse, 'postId' | 'title' | 'createdAt'>> & UserProfileCommunityPostResponse =>
            typeof post.postId === 'string' &&
            typeof post.title === 'string' &&
            typeof post.createdAt === 'string',
        )
        .map((post) => ({
          postId: post.postId,
          title: post.title,
          excerpt: post.excerpt ?? '',
          tags: normalizeTags(post.tags),
          createdAt: post.createdAt,
          updatedAt: typeof post.updatedAt === 'string' ? post.updatedAt : undefined,
          likeCount: post.likeCount ?? 0,
          commentCount: post.commentCount ?? 0,
        }));
    },
  );
}

async function fetchProfileCommunityComments(path: string): Promise<ProfileCommunityComment[]> {
  return requestCommunity(
    path,
    { method: 'GET' },
    '프로필 커뮤니티 댓글 조회에 실패했다.',
    (data) => {
      const response = data as UserProfileCommunityCommentsResponse;
      if (!Array.isArray(response.comments)) {
        throw new Error();
      }

      return response.comments
        .filter(
          (comment): comment is Required<Pick<UserProfileCommunityCommentResponse, 'commentId' | 'postId' | 'postTitle' | 'content' | 'createdAt' | 'reply'>> & UserProfileCommunityCommentResponse =>
            typeof comment.commentId === 'number' &&
            typeof comment.postId === 'string' &&
            typeof comment.postTitle === 'string' &&
            typeof comment.content === 'string' &&
            typeof comment.createdAt === 'string' &&
            typeof comment.reply === 'boolean',
        )
        .map((comment) => ({
          commentId: comment.commentId,
          postId: comment.postId,
          postTitle: comment.postTitle,
          content: comment.content,
          createdAt: comment.createdAt,
          reply: comment.reply,
        }));
    },
  );
}

export function fetchMyCommunityPosts() {
  return fetchProfileCommunityPosts('/profile/me/community/posts');
}

export function fetchCommunityPostsByUser(handle: string) {
  return fetchProfileCommunityPosts(`/profiles/${encodeURIComponent(handle)}/community/posts`);
}

export function fetchMyLikedPosts() {
  return fetchProfileCommunityPosts('/profile/me/community/liked-posts');
}

export function fetchLikedPostsByUser(handle: string) {
  return fetchProfileCommunityPosts(`/profiles/${encodeURIComponent(handle)}/community/liked-posts`);
}

export function fetchMyCommunityComments() {
  return fetchProfileCommunityComments('/profile/me/community/comments');
}

export function fetchCommunityCommentsByUser(handle: string) {
  return fetchProfileCommunityComments(`/profiles/${encodeURIComponent(handle)}/community/comments`);
}

export function fetchMyLikedComments() {
  return fetchProfileCommunityComments('/profile/me/community/liked-comments');
}

export function fetchLikedCommentsByUser(handle: string) {
  return fetchProfileCommunityComments(`/profiles/${encodeURIComponent(handle)}/community/liked-comments`);
}
