import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw, Search, TicketPercent } from "lucide-react";
import { getApiError } from "../../api/client";
import { voucherApi } from "../../api/voucherApi";
import { Button, DataTable, EmptyState, Field, Input, PageHeader, Panel, Price, Select, StatusBadge } from "../../components/ui";
import { formatDate } from "../../lib/format";
import { useToast } from "../../state/ToastContext";
import type { VoucherType } from "../../types";

export function AdminVouchersPage() {
  const [query, setQuery] = useState("");
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [type, setType] = useState<VoucherType>("PERCENT");
  const [value, setValue] = useState(10);
  const [minOrderAmount, setMinOrderAmount] = useState(0);
  const [maxDiscountAmount, setMaxDiscountAmount] = useState(0);
  const [usageLimit, setUsageLimit] = useState(100);
  const [active, setActive] = useState(true);
  const toast = useToast();
  const queryClient = useQueryClient();
  const vouchers = useQuery({ queryKey: ["admin", "vouchers"], queryFn: voucherApi.adminList });
  const filtered = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (vouchers.data ?? []).filter((voucher) => !keyword || [voucher.code, voucher.name, voucher.type].some((item) => item.toLowerCase().includes(keyword)));
  }, [query, vouchers.data]);
  const create = useMutation({
    mutationFn: () => voucherApi.adminCreate({
      code,
      name,
      type,
      value,
      minOrderAmount,
      maxDiscountAmount: maxDiscountAmount || undefined,
      usageLimit,
      active
    }),
    onSuccess: async () => {
      toast.success("Da tao voucher");
      setCode("");
      setName("");
      setType("PERCENT");
      setValue(10);
      setMinOrderAmount(0);
      setMaxDiscountAmount(0);
      setUsageLimit(100);
      setActive(true);
      await queryClient.invalidateQueries({ queryKey: ["admin", "vouchers"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const toggle = useMutation({
    mutationFn: ({ id, nextActive }: { id: string; nextActive: boolean }) => voucherApi.setActive(id, nextActive),
    onSuccess: async () => {
      toast.success("Da cap nhat voucher");
      await queryClient.invalidateQueries({ queryKey: ["admin", "vouchers"] });
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
        title="Vouchers"
        actions={<Button variant="secondary" onClick={() => vouchers.refetch()} loading={vouchers.isFetching}><RefreshCw size={16} /> Refresh</Button>}
      />
      <div className="metric-grid compact">
        <Panel><span>Total</span><strong>{vouchers.data?.length ?? 0}</strong></Panel>
        <Panel><span>Active</span><strong>{vouchers.data?.filter((voucher) => voucher.active).length ?? 0}</strong></Panel>
        <Panel><span>Visible</span><strong>{filtered.length}</strong></Panel>
      </div>
      <Panel>
        <div className="admin-toolbar admin-toolbar-wrap">
          <div className="admin-search">
            <Search size={17} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search code, name, type" />
          </div>
          <span className="toolbar-count">{filtered.length} vouchers</span>
        </div>
        {vouchers.isError ? <EmptyState title="Khong lay duoc vouchers" detail={getApiError(vouchers.error)} /> : null}
        <DataTable>
          <thead><tr><th>Code</th><th>Type</th><th>Value</th><th>Min order</th><th>Max discount</th><th>Usage</th><th>Valid until</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {filtered.map((voucher) => (
              <tr key={voucher.id}>
                <td><strong>{voucher.code}</strong><div className="table-subtext">{voucher.name}</div></td>
                <td><StatusBadge value={voucher.type} /></td>
                <td>{voucher.type === "PERCENT" ? `${voucher.value}%` : <Price value={voucher.value} />}</td>
                <td><Price value={voucher.minOrderAmount} /></td>
                <td>{voucher.maxDiscountAmount ? <Price value={voucher.maxDiscountAmount} /> : "-"}</td>
                <td>{voucher.usedCount}/{voucher.usageLimit}</td>
                <td>{formatDate(voucher.endsAt)}</td>
                <td><StatusBadge value={voucher.active ? "ACTIVE" : "INACTIVE"} /></td>
                <td>
                  <Button variant="secondary" loading={toggle.isPending} onClick={() => toggle.mutate({ id: voucher.id, nextActive: !voucher.active })}>
                    {voucher.active ? "Disable" : "Enable"}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      </Panel>
      <Panel>
        <h2><TicketPercent size={17} /> Create voucher</h2>
        <form className="form-grid admin-create-form" onSubmit={submit}>
          <Field label="Code"><Input value={code} onChange={(event) => setCode(event.target.value.toUpperCase())} required placeholder="WELCOME10" /></Field>
          <Field label="Name"><Input value={name} onChange={(event) => setName(event.target.value)} required placeholder="Welcome discount" /></Field>
          <Field label="Type">
            <Select value={type} onChange={(event) => setType(event.target.value as VoucherType)}>
              <option value="PERCENT">Percent</option>
              <option value="FIXED">Fixed amount</option>
            </Select>
          </Field>
          <Field label={type === "PERCENT" ? "Percent value" : "Discount amount"}><Input type="number" min="1" max={type === "PERCENT" ? 100 : undefined} value={value || ""} onChange={(event) => setValue(Number(event.target.value))} required /></Field>
          <Field label="Min order"><Input type="number" min="0" value={minOrderAmount || ""} onChange={(event) => setMinOrderAmount(Number(event.target.value))} /></Field>
          <Field label="Max discount"><Input type="number" min="0" value={maxDiscountAmount || ""} onChange={(event) => setMaxDiscountAmount(Number(event.target.value))} /></Field>
          <Field label="Usage limit"><Input type="number" min="1" value={usageLimit || ""} onChange={(event) => setUsageLimit(Number(event.target.value))} required /></Field>
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
