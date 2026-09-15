import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BadgePercent, RefreshCw, Search } from "lucide-react";
import { catalogApi } from "../../api/catalogApi";
import { getApiError } from "../../api/client";
import { productDiscountApi } from "../../api/productDiscountApi";
import { Button, DataTable, EmptyState, Field, Input, PageHeader, Panel, Price, Select, StatusBadge } from "../../components/ui";
import { formatDate } from "../../lib/format";
import { useToast } from "../../state/ToastContext";
import type { PromotionTargetType, VoucherType } from "../../types";

const toApiDateTime = (value: string) => (value ? new Date(value).toISOString() : undefined);

export function AdminProductDiscountsPage() {
  const [query, setQuery] = useState("");
  const [name, setName] = useState("");
  const [targetType, setTargetType] = useState<PromotionTargetType>("PRODUCT");
  const [targetId, setTargetId] = useState("");
  const [type, setType] = useState<VoucherType>("PERCENT");
  const [value, setValue] = useState(10);
  const [maxDiscountAmount, setMaxDiscountAmount] = useState(0);
  const [startsAt, setStartsAt] = useState("");
  const [endsAt, setEndsAt] = useState("");
  const [active, setActive] = useState(true);
  const toast = useToast();
  const queryClient = useQueryClient();
  const discounts = useQuery({ queryKey: ["admin", "product-discounts"], queryFn: productDiscountApi.adminList });
  const products = useQuery({ queryKey: ["products"], queryFn: catalogApi.products });
  const categories = useQuery({ queryKey: ["categories"], queryFn: catalogApi.categories });
  const variants = useQuery({ queryKey: ["variants"], queryFn: catalogApi.variants });

  const targetOptions = useMemo(() => {
    if (targetType === "CATEGORY") return (categories.data ?? []).map((item) => ({ value: item.id, label: item.name }));
    if (targetType === "VARIANT_SIZE") {
      return (variants.data ?? []).flatMap((variant) =>
        (variant.sizes ?? []).map((size) => ({ value: size.id, label: `${variant.productName || "Product"} / ${variant.color} / ${size.size}` }))
      );
    }
    return (products.data ?? []).map((item) => ({ value: item.id, label: item.name }));
  }, [categories.data, products.data, targetType, variants.data]);

  const filtered = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (discounts.data ?? []).filter((discount) => !keyword || [discount.name, discount.targetType, discount.targetId].some((item) => item.toLowerCase().includes(keyword)));
  }, [discounts.data, query]);

  const create = useMutation({
    mutationFn: () => productDiscountApi.adminCreate({
      name,
      targetType,
      targetId,
      type,
      value,
      maxDiscountAmount: maxDiscountAmount || undefined,
      startsAt: toApiDateTime(startsAt),
      endsAt: toApiDateTime(endsAt),
      active
    }),
    onSuccess: async () => {
      toast.success("Da tao product discount");
      setName("");
      setTargetId("");
      setType("PERCENT");
      setValue(10);
      setMaxDiscountAmount(0);
      setStartsAt("");
      setEndsAt("");
      setActive(true);
      await queryClient.invalidateQueries({ queryKey: ["admin", "product-discounts"] });
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      await queryClient.invalidateQueries({ queryKey: ["variants"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const toggle = useMutation({
    mutationFn: ({ id, nextActive }: { id: string; nextActive: boolean }) => productDiscountApi.setActive(id, nextActive),
    onSuccess: async () => {
      toast.success("Da cap nhat product discount");
      await queryClient.invalidateQueries({ queryKey: ["admin", "product-discounts"] });
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      await queryClient.invalidateQueries({ queryKey: ["variants"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate();
  };

  return (
    <div className="admin-page">
      <PageHeader
        title="Product discounts"
        actions={<Button variant="secondary" onClick={() => discounts.refetch()} loading={discounts.isFetching}><RefreshCw size={16} /> Refresh</Button>}
      />
      <Panel>
        <div className="admin-toolbar admin-toolbar-wrap">
          <div className="admin-search">
            <Search size={17} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search discount, target" />
          </div>
          <span className="toolbar-count">{filtered.length} discounts</span>
        </div>
        {discounts.isError ? <EmptyState title="Khong lay duoc discounts" detail={getApiError(discounts.error)} /> : null}
        <DataTable>
          <thead><tr><th>Name</th><th>Target</th><th>Value</th><th>Max</th><th>Ends</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {filtered.map((discount) => (
              <tr key={discount.id}>
                <td><strong>{discount.name}</strong></td>
                <td><StatusBadge value={discount.targetType} /><div className="table-subtext">{discount.targetId}</div></td>
                <td>{discount.type === "PERCENT" ? `${discount.value}%` : <Price value={discount.value} />}</td>
                <td>{discount.maxDiscountAmount ? <Price value={discount.maxDiscountAmount} /> : "-"}</td>
                <td>{formatDate(discount.endsAt)}</td>
                <td><StatusBadge value={discount.active ? "ACTIVE" : "INACTIVE"} /></td>
                <td><Button variant="secondary" loading={toggle.isPending} onClick={() => toggle.mutate({ id: discount.id, nextActive: !discount.active })}>{discount.active ? "Disable" : "Enable"}</Button></td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      </Panel>
      <Panel>
        <h2><BadgePercent size={17} /> Create product discount</h2>
        <form className="form-grid admin-create-form" onSubmit={submit}>
          <Field label="Name"><Input value={name} onChange={(event) => setName(event.target.value)} required placeholder="Weekend markdown" /></Field>
          <Field label="Target type">
            <Select value={targetType} onChange={(event) => { setTargetType(event.target.value as PromotionTargetType); setTargetId(""); }}>
              <option value="PRODUCT">Product</option>
              <option value="CATEGORY">Category</option>
              <option value="VARIANT_SIZE">Variant size</option>
            </Select>
          </Field>
          <Field label="Target">
            <Select value={targetId} onChange={(event) => setTargetId(event.target.value)} required>
              <option value="">Select target</option>
              {targetOptions.map((item) => <option value={item.value} key={item.value}>{item.label}</option>)}
            </Select>
          </Field>
          <Field label="Type">
            <Select value={type} onChange={(event) => setType(event.target.value as VoucherType)}>
              <option value="PERCENT">Percent</option>
              <option value="FIXED">Fixed amount</option>
            </Select>
          </Field>
          <Field label={type === "PERCENT" ? "Percent value" : "Discount amount"}><Input type="number" min="1" max={type === "PERCENT" ? 100 : undefined} value={value || ""} onChange={(event) => setValue(Number(event.target.value))} required /></Field>
          <Field label="Max discount"><Input type="number" min="0" value={maxDiscountAmount || ""} onChange={(event) => setMaxDiscountAmount(Number(event.target.value))} /></Field>
          <Field label="Starts at"><Input type="datetime-local" value={startsAt} onChange={(event) => setStartsAt(event.target.value)} /></Field>
          <Field label="Ends at"><Input type="datetime-local" value={endsAt} onChange={(event) => setEndsAt(event.target.value)} /></Field>
          <label className="check-row">
            <span>Active</span>
            <input type="checkbox" checked={active} onChange={(event) => setActive(event.target.checked)} />
          </label>
          <Button loading={create.isPending} type="submit">Create</Button>
        </form>
      </Panel>
    </div>
  );
}
