import { type KeyboardEvent as ReactKeyboardEvent } from 'react';
import { formatBoardDate } from '@/shared/lib/formatters';
import { getProfilePath, navigate } from '@/shared/config/navigation';
import { useUiText } from '@/shared/config/ui-text';
import type { CommunityComment } from '@/shared/api/domain';

interface CommunityCommentThreadProps {
  comment: CommunityComment;
  depth?: number;
  activeReplyId: string | null;
  replyDrafts: Record<string, string>;
  onToggleReply: (commentId: string) => void;
  onChangeReplyDraft: (commentId: string, value: string) => void;
  onSubmitReply: (commentId: string) => void;
  onToggleLike: (commentId: string) => void;
}

function LikeIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 13.3 3.5 9.1a2.8 2.8 0 0 1 4-4L8 5.6l.5-.5a2.8 2.8 0 0 1 4 4L8 13.3Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}


function SendIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M2.7 7.6 13.3 3 9.4 13l-2.2-3.2-4.5-2.2Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function CommunityCommentThread({
  comment,
  depth = 0,
  activeReplyId,
  replyDrafts,
  onToggleReply,
  onChangeReplyDraft,
  onSubmitReply,
  onToggleLike,
}: CommunityCommentThreadProps) {
  const { text } = useUiText();
  const isReplyComposerOpen = activeReplyId === comment.id;
  const canReply = depth < 3;

  function handleReplyDraftKeyDown(event: ReactKeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      onSubmitReply(comment.id);
    }
  }

  const articleClassName = [
    'community-comment-item',
    depth > 0 ? 'is-reply' : '',
    `depth-${Math.min(depth, 3)}`,
    comment.replies.length > 0 ? 'has-replies' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <article id={`community-comment-${comment.id}`} className={articleClassName} tabIndex={-1}>
      <div className="community-comment-meta">
        <div className="community-comment-author-group">
          <button
            type="button"
            className="community-author-button"
            onClick={() => navigate(getProfilePath(comment.authorHandle))}
          >
            <strong className="community-comment-author">{comment.authorHandle}</strong>
          </button>
          <span className="community-comment-time">{formatBoardDate(comment.createdAt)}</span>
          <button
            type="button"
            className={`community-comment-like-button ${comment.likedByCurrentUser ? 'is-liked' : ''}`.trim()}
            onClick={() => onToggleLike(comment.id)}
            aria-label={comment.likedByCurrentUser ? text('COMMUNITY_COMMENT_UNLIKE_LABEL', '댓글 좋아요 취소') : text('COMMUNITY_COMMENT_LIKE_LABEL', '댓글 좋아요')}
          >
            <LikeIcon />
            <span>{comment.likes}</span>
          </button>
        </div>
      </div>

      <p className="community-comment-content">{comment.content}</p>

      {canReply ? (
        <div className="community-comment-inline-actions">
          <button
            type="button"
            className="community-comment-inline-reply"
            onClick={() => onToggleReply(comment.id)}
          >
            {isReplyComposerOpen ? text('COMMON_CLOSE_BUTTON', '닫기') : text('COMMUNITY_REPLY_TOGGLE_BUTTON', '댓글 달기')}
          </button>
        </div>
      ) : null}

      {isReplyComposerOpen || comment.replies.length > 0 ? (
        <div className="community-comment-branch">
          {isReplyComposerOpen ? (
            <div className="community-reply-compose">
              <div className="community-reply-compose-field">
                <textarea
                  className="text-field community-comment-textarea community-comment-textarea-reply is-reply"
                  value={replyDrafts[comment.id] ?? ''}
                  onChange={(event) => onChangeReplyDraft(comment.id, event.target.value)}
                  onKeyDown={handleReplyDraftKeyDown}
                  placeholder={text('COMMUNITY_COMMENT_PLACEHOLDER', '댓글 추가')}
                />
                <button
                  type="button"
                  className="community-comment-submit-icon community-reply-submit-icon"
                  onClick={() => onSubmitReply(comment.id)}
                  aria-label={text('COMMUNITY_REPLY_SUBMIT_BUTTON', '대댓글 등록')}
                >
                  <SendIcon />
                </button>
              </div>
            </div>
          ) : null}

          {comment.replies.length > 0 ? (
            <div className="community-comment-replies">
              {comment.replies.map((reply) => (
                <CommunityCommentThread
                  key={reply.id}
                  comment={reply}
                  depth={depth + 1}
                  activeReplyId={activeReplyId}
                  replyDrafts={replyDrafts}
                  onToggleReply={onToggleReply}
                  onChangeReplyDraft={onChangeReplyDraft}
                  onSubmitReply={onSubmitReply}
                  onToggleLike={onToggleLike}
                />
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </article>
  );
}
