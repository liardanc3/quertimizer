import { getApiBaseUrl } from './authApi';
import { createApiErrorFromResponse, getUiTextValue } from './uiText';
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
  contentJson?: string;
  imageIds?: string[];
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

interface UserProfileCommunityActivityResponse {
  activityType?: string;
  postId?: string;
  postTitle?: string;
  commentId?: number | null;
  content?: string;
  happenedAt?: string;
}

interface UserProfileCommunityActivitiesResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  activities?: UserProfileCommunityActivityResponse[];
}

const communityGetRequestPromises = new Map<string, Promise<unknown>>();

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
  contentJson: string;
  imageIds: string[];
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
  category: CommunityPostSummary['category'];
  tags: string[];
  contentJson: string;
  plainTextSummary: string;
  imageIds: string[];
}

export interface CommunityUploadedImageResponse {
  imageId?: string;
  imageUrl?: string;
}

export interface CommunityUploadedImage {
  imageId: string;
  imageUrl: string;
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

export interface ProfileCommunityActivity {
  activityType: 'post' | 'likedPost' | 'comment' | 'likedComment';
  postId: string;
  postTitle: string;
  commentId?: number;
  content: string;
  happenedAt: string;
}

export interface ProfileCommunityActivityPage {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  activities: ProfileCommunityActivity[];
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
    contentJson: undefined,
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

async function executeCommunityRequest<T>(
  path: string,
  init: RequestInit,
  fallbackMessage: string,
  normalize: (data: unknown) => T,
): Promise<T> {
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
    throw await createApiErrorFromResponse(response, fallbackMessage);
  }

  try {
    const data = (await response.json()) as unknown;
    return normalize(data);
  } catch {
    throw new Error(fallbackMessage);
  }
}

function requestCommunity<T>(
  path: string,
  init: RequestInit,
  fallbackMessage: string,
  normalize: (data: unknown) => T,
): Promise<T> {
  const requestMethod = (init.method ?? 'GET').toUpperCase();

  if (requestMethod !== 'GET') {
    return executeCommunityRequest(path, init, fallbackMessage, normalize);
  }

  const requestKey = `${requestMethod}:${path}`;
  const inFlightRequest = communityGetRequestPromises.get(requestKey);
  if (inFlightRequest != null) {
    return inFlightRequest as Promise<T>;
  }

  const nextRequest = executeCommunityRequest(path, init, fallbackMessage, normalize).finally(() => {
    communityGetRequestPromises.delete(requestKey);
  });

  communityGetRequestPromises.set(requestKey, nextRequest);
  return nextRequest;
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
    getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'),
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
    getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'),
    (data) => {
      const post = data as CommunityPostDetailResponse;
      if (
        typeof post.postId !== 'string' ||
        typeof post.title !== 'string' ||
        typeof post.authorId !== 'string' ||
        typeof post.createdAt !== 'string' ||
        typeof post.contentJson !== 'string' ||
        !Array.isArray(post.comments)
      ) {
        throw new Error();
      }

      return {
        postId: post.postId,
        title: post.title,
        authorHandle: post.authorId,
        excerpt: post.excerpt ?? '',
        content: post.excerpt ?? '',
        contentJson: post.contentJson,
        imageIds: Array.isArray(post.imageIds) ? post.imageIds.filter((imageId): imageId is string => typeof imageId === 'string') : [],
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
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMUNITY_POST_SAVE_FAIL_MESSAGE', '게시글을 저장하지 못했습니다.'));
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
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMUNITY_EDIT_FAIL_MESSAGE', '게시글을 수정하지 못했습니다.'));
  }
}

export async function uploadCommunityImage(file: File): Promise<CommunityUploadedImage> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${getApiBaseUrl()}/community/images`, {
    method: 'POST',
    credentials: 'include',
    body: formData,
  });

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMUNITY_EDITOR_IMAGE_UPLOAD_FAIL_MESSAGE', '이미지 업로드에 실패했습니다.'));
  }

  const uploadedImage = (await response.json()) as CommunityUploadedImageResponse;
  if (typeof uploadedImage.imageId !== 'string' || typeof uploadedImage.imageUrl !== 'string') {
    throw new Error(getUiTextValue('COMMUNITY_EDITOR_IMAGE_UPLOAD_PARSE_FAIL_MESSAGE', '이미지 업로드 응답 형식이 올바르지 않습니다.'));
  }

  return {
    imageId: uploadedImage.imageId,
    imageUrl: uploadedImage.imageUrl.startsWith('http')
      ? uploadedImage.imageUrl
      : `${getApiBaseUrl()}${uploadedImage.imageUrl}`,
  };
}

export async function deleteCommunityPost(postId: string) {
  const response = await fetch(`${getApiBaseUrl()}/community/posts/${encodeURIComponent(postId)}`, {
    method: 'DELETE',
    credentials: 'include',
  });

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMUNITY_DELETE_FAIL_MESSAGE', '게시글을 삭제하지 못했습니다.'));
  }
}

export async function toggleCommunityPostLike(postId: string): Promise<CommunityReaction> {
  return requestCommunity(
    `/community/posts/${encodeURIComponent(postId)}/likes`,
    { method: 'POST' },
    getUiTextValue('COMMUNITY_POST_LIKE_FAIL_MESSAGE', '좋아요 처리에 실패했습니다.'),
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
    getUiTextValue('COMMUNITY_COMMENT_SAVE_FAIL_MESSAGE', '댓글을 등록하지 못했습니다.'),
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
    getUiTextValue('COMMUNITY_COMMENT_LIKE_FAIL_MESSAGE', '댓글 좋아요 처리에 실패했습니다.'),
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
    getUiTextValue('COMMUNITY_TAG_AUTOCOMPLETE_LOAD_FAIL_MESSAGE', '태그 자동완성을 불러오지 못했습니다.'),
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
    getUiTextValue('PROFILE_COMMUNITY_POSTS_LOAD_FAIL_MESSAGE', '프로필 커뮤니티 게시글을 불러오지 못했습니다.'),
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
    getUiTextValue('PROFILE_COMMUNITY_COMMENTS_LOAD_FAIL_MESSAGE', '프로필 커뮤니티 댓글을 불러오지 못했습니다.'),
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

async function fetchProfileCommunityActivities(path: string, page: number, pageSize: number): Promise<ProfileCommunityActivityPage> {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });

  return requestCommunity(
    `${path}?${params.toString()}`,
    { method: 'GET' },
    getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'),
    (data) => {
      const response = data as UserProfileCommunityActivitiesResponse;
      if (!Array.isArray(response.activities)) {
        throw new Error();
      }

      return {
        currentPage: response.currentPage ?? 1,
        pageSize: response.pageSize ?? pageSize,
        totalCount: response.totalCount ?? 0,
        totalPages: Math.max(1, response.totalPages ?? 1),
        activities: response.activities
          .filter(
            (activity): activity is Required<Pick<UserProfileCommunityActivityResponse, 'activityType' | 'postId' | 'postTitle' | 'happenedAt'>> & UserProfileCommunityActivityResponse =>
              (activity.activityType === 'post' ||
                activity.activityType === 'likedPost' ||
                activity.activityType === 'comment' ||
                activity.activityType === 'likedComment') &&
              typeof activity.postId === 'string' &&
              typeof activity.postTitle === 'string' &&
              typeof activity.happenedAt === 'string',
          )
          .map((activity) => ({
            activityType: activity.activityType as ProfileCommunityActivity['activityType'],
            postId: activity.postId,
            postTitle: activity.postTitle,
            commentId: typeof activity.commentId === 'number' ? activity.commentId : undefined,
            content: activity.content ?? '',
            happenedAt: activity.happenedAt,
          })),
      };
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

export function fetchMyCommunityActivities(page: number, pageSize: number) {
  return fetchProfileCommunityActivities('/profile/me/community/activities', page, pageSize);
}

export function fetchCommunityActivitiesByUser(handle: string, page: number, pageSize: number) {
  return fetchProfileCommunityActivities(`/profiles/${encodeURIComponent(handle)}/community/activities`, page, pageSize);
}
