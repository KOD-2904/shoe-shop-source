import { FormEvent, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Star, Trash2 } from "lucide-react";
import { getApiError } from "../api/client";
import { reviewApi } from "../api/reviewApi";
import { Button, EmptyState, Field, Panel, Textarea } from "./ui";
import { formatDate } from "../lib/format";
import { useAuth } from "../state/AuthContext";
import { useToast } from "../state/ToastContext";

export function ProductReviews({ productId, orderItemId }: { productId: string; orderItemId?: string }) {
  const { user, isAuthenticated } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [imageUrls, setImageUrls] = useState("");
  const reviews = useQuery({ queryKey: ["reviews", productId], queryFn: () => reviewApi.byProduct(productId) });
  const myReview = reviews.data?.reviews.find((review) =>
    orderItemId ? review.orderItemId === orderItemId : review.userId === user?.id
  );

  useEffect(() => {
    if (!myReview) return;
    setRating(myReview.rating);
    setComment(myReview.comment || "");
    setImageUrls((myReview.imageUrls ?? []).join("\n"));
  }, [myReview]);

  const save = useMutation({
    mutationFn: () => reviewApi.saveMine(productId, {
      rating,
      comment: comment.trim() || undefined,
      orderItemId,
      imageUrls: imageUrls
        .split(/[\n,]+/)
        .map((url) => url.trim())
        .filter(Boolean)
    }),
    onSuccess: async () => {
      toast.success("Da luu review");
      await queryClient.invalidateQueries({ queryKey: ["reviews", productId] });
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const remove = useMutation({
    mutationFn: () => reviewApi.deleteMine(productId, orderItemId),
    onSuccess: async () => {
      toast.success("Da xoa review");
      setRating(5);
      setComment("");
      setImageUrls("");
      await queryClient.invalidateQueries({ queryKey: ["reviews", productId] });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };

  return (
    <Panel className="reviews-panel">
      <div className="reviews-heading">
        <div>
          <p className="eyebrow">Customer feedback</p>
          <h2>Reviews</h2>
        </div>
        <div className="rating-summary" aria-label={`${reviews.data?.averageRating || 0} average rating`}>
          <Star size={18} fill="currentColor" />
          <strong>{reviews.data?.averageRating?.toFixed(1) || "0.0"}</strong>
          <span>{reviews.data?.reviewCount || 0} reviews</span>
        </div>
      </div>

      {isAuthenticated ? (
        <form className="review-form" onSubmit={submit}>
          <Field label={myReview ? "Update your rating" : "Your rating"}>
            <div className="rating-picker">
              {[1, 2, 3, 4, 5].map((value) => (
                <button
                  type="button"
                  className={value <= rating ? "active" : ""}
                  aria-label={`${value} stars`}
                  key={value}
                  onClick={() => setRating(value)}
                >
                  <Star size={20} fill="currentColor" />
                </button>
              ))}
            </div>
          </Field>
          <Field label="Review">
            <Textarea value={comment} onChange={(event) => setComment(event.target.value)} maxLength={1000} placeholder="Share fit, comfort, sizing, material or delivery notes." />
          </Field>
          <Field label="Review image URLs">
            <Textarea value={imageUrls} onChange={(event) => setImageUrls(event.target.value)} maxLength={3000} placeholder="Paste one image URL per line." />
          </Field>
          <div className="button-row">
            <Button type="submit" loading={save.isPending}>{myReview ? "Update review" : "Submit review"}</Button>
            {myReview ? (
              <Button type="button" variant="danger" loading={remove.isPending} onClick={() => remove.mutate()}>
                <Trash2 size={16} /> Delete
              </Button>
            ) : null}
          </div>
          <p className="review-rule">{orderItemId ? "This review is linked to the delivered order item." : "Only delivered purchases can be reviewed."}</p>
        </form>
      ) : (
        <p className="muted">Login after a delivered purchase to leave a review.</p>
      )}

      {reviews.isError ? <EmptyState title="Khong lay duoc review" detail={getApiError(reviews.error)} /> : null}
      {reviews.data?.reviews.length === 0 ? <EmptyState title="Chua co review" detail="Be the first verified customer to review this pair." /> : null}
      <div className="review-list">
        {reviews.data?.reviews.map((review) => (
          <article className="review-item" key={review.id}>
            <div className="row-between">
              <strong>{review.reviewer}</strong>
              <span className="review-stars" aria-label={`${review.rating} stars`}>
                {Array.from({ length: 5 }, (_, index) => (
                  <Star size={14} fill="currentColor" key={index} className={index < review.rating ? "active" : ""} />
                ))}
              </span>
            </div>
            {review.comment ? <p>{review.comment}</p> : null}
            {review.imageUrls?.length ? (
              <div className="review-images">
                {review.imageUrls.map((url) => <img src={url} alt="Customer review" loading="lazy" key={url} />)}
              </div>
            ) : null}
            <small>{formatDate(review.updatedAt || review.createdAt)}</small>
          </article>
        ))}
      </div>
    </Panel>
  );
}
