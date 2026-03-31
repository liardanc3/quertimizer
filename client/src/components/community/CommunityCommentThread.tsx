import type { CommunityComment } from '../../types/domain';

interface CommunityCommentThreadProps {
  comment: CommunityComment;
  depth?: number;
  activeReplyId: string | null;
  replyDrafts: Record<string, string>;
  onToggleReply: (commentId: string) => void;
  onChangeReplyDraft: (commentId: string, value: string) => void;
  onSubmitReply: (commentId: string) => void;
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
}: CommunityCommentThreadProps) {
  const isReplyComposerOpen = activeReplyId === comment.id;
  const canReply = depth === 0;

  return (
    <article className={`community-comment-item ${depth > 0 ? 'is-reply' : ''}`.trim()}>
      <div className="community-comment-meta">
        <div className="community-comment-author-group">
          <strong className="community-comment-author">@{comment.authorHandle}</strong>
          <span className="community-comment-time">{formatBoardDate(comment.createdAt)}</span>
        </div>

        <div className="community-comment-actions">
          <span className="community-comment-like">좋아요 {comment.likes}</span>
          {canReply ? (
            <button
              type="button"
              className="btn text inline community-comment-reply-button"
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
            placeholder="대댓글을 입력하세요."
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
            />
          ))}
        </div>
      ) : null}
    </article>
  );
}
