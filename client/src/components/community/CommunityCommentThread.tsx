import { getProfilePath, navigate } from '../../lib/navigation';
import type { CommunityComment } from '../../types/domain';

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

function formatBoardDate(value: string) {
  const date = new Date(value);
  const year = String(date.getFullYear()).slice(-2);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}`;
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
  const isReplyComposerOpen = activeReplyId === comment.id;
  const canReply = depth === 0;

  return (
    <article className={`community-comment-item ${depth > 0 ? 'is-reply' : ''}`.trim()}>
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
        </div>

        <div className="community-comment-actions">
          <button
            type="button"
            className="community-comment-reply-button"
            onClick={() => onToggleLike(comment.id)}
          >
            {comment.likedByCurrentUser ? `좋아요 취소 ${comment.likes}` : `좋아요 ${comment.likes}`}
          </button>
          {canReply ? (
            <button
              type="button"
              className="community-comment-reply-button"
              onClick={() => onToggleReply(comment.id)}
            >
              {isReplyComposerOpen ? '대댓글 닫기' : '대댓글'}
            </button>
          ) : null}
        </div>
      </div>

      <p className="community-comment-content">{comment.content}</p>

      {isReplyComposerOpen ? (
        <div className="community-reply-compose">
          <textarea
            className="text-field community-comment-textarea is-reply"
            value={replyDrafts[comment.id] ?? ''}
            onChange={(event) => onChangeReplyDraft(comment.id, event.target.value)}
            placeholder="대댓글을 입력해."
          />
          <div className="community-reply-actions">
            <button type="button" className="btn secondary" onClick={() => onSubmitReply(comment.id)}>
              대댓글 등록
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
    </article>
  );
}
