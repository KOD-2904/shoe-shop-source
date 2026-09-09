import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Edit3, Trash2 } from "lucide-react";
import { addressApi } from "../api/addressApi";
import { getApiError } from "../api/client";
import { Button, EmptyState, Field, Input, PageHeader, Panel, Select, StatusBadge } from "../components/ui";
import { cacheTimes } from "../lib/queryCache";
import { useAuth } from "../state/AuthContext";
import { useToast } from "../state/ToastContext";
import type { AddressResponse } from "../types";

const emptyForm = {
  receiverName: "",
  phoneNumber: "",
  detailAddress: "",
  provinceId: "",
  districtId: "",
  wardCode: "",
  isDefault: true
};

export function AddressesPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const returnTo = params.get("return");
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<AddressResponse | null>(null);
  const [form, setForm] = useState(emptyForm);
  const addresses = useQuery({ queryKey: ["addresses"], queryFn: addressApi.mine });
  const provinces = useQuery({ queryKey: ["ghn", "provinces"], queryFn: addressApi.provinces, ...cacheTimes.shippingLocation });
  const districts = useQuery({
    queryKey: ["ghn", "districts", form.provinceId],
    queryFn: () => addressApi.districts(Number(form.provinceId)),
    enabled: Boolean(form.provinceId),
    ...cacheTimes.shippingLocation
  });
  const wards = useQuery({
    queryKey: ["ghn", "wards", form.districtId],
    queryFn: () => addressApi.wards(Number(form.districtId)),
    enabled: Boolean(form.districtId),
    ...cacheTimes.shippingLocation
  });
  const selectedProvince = useMemo(() => provinces.data?.find((province) => String(province.ProvinceID) === form.provinceId), [form.provinceId, provinces.data]);
  const selectedDistrict = useMemo(() => districts.data?.find((district) => String(district.DistrictID) === form.districtId), [form.districtId, districts.data]);
  const selectedWard = useMemo(() => wards.data?.find((ward) => ward.WardCode === form.wardCode), [form.wardCode, wards.data]);

  useEffect(() => {
    if (!editing) return;
    setForm({
      receiverName: editing.receiverName || "",
      phoneNumber: editing.phoneNumber || "",
      detailAddress: editing.detailAddress || "",
      provinceId: editing.provinceId ? String(editing.provinceId) : "",
      districtId: editing.districtId ? String(editing.districtId) : "",
      wardCode: editing.wardCode || "",
      isDefault: Boolean(editing.isDefault)
    });
  }, [editing]);

  const save = useMutation({
    mutationFn: async () => {
      if (!selectedProvince || !selectedDistrict || !selectedWard) throw new Error("Vui long chon day du tinh, huyen, xa");
      const body = {
        isDefault: form.isDefault,
        receiverName: form.receiverName,
        phoneNumber: form.phoneNumber,
        detailAddress: form.detailAddress,
        provinceId: selectedProvince.ProvinceID,
        districtId: selectedDistrict.DistrictID,
        wardCode: selectedWard.WardCode,
        provinceName: selectedProvince.ProvinceName,
        districtName: selectedDistrict.DistrictName,
        wardName: selectedWard.WardName
      };
      return editing ? addressApi.update(editing.id, body) : addressApi.add(body);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["addresses"] });
      toast.success(editing ? "Da cap nhat dia chi" : "Da them dia chi");
      setEditing(null);
      setForm(emptyForm);
      if (returnTo) navigate(returnTo, { replace: true });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const setDefault = useMutation({
    mutationFn: addressApi.setDefault,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["addresses"] });
      toast.success("Da doi dia chi mac dinh");
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const removeAddress = useMutation({
    mutationFn: addressApi.remove,
    onSuccess: async (deleted) => {
      await queryClient.invalidateQueries({ queryKey: ["addresses"] });
      if (editing?.id === deleted.id) {
        setEditing(null);
        setForm(emptyForm);
      }
      toast.success("Da xoa dia chi");
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };

  return (
    <div>
      <PageHeader title="Dia chi nhan hang" />
      <div className="account-grid">
        <Panel>
          <h2>{editing ? "Sua dia chi" : "Them dia chi"}</h2>
          <form className="form-grid" onSubmit={submit}>
            <Field label="Nguoi nhan">
              <Input value={form.receiverName} onChange={(event) => setForm((current) => ({ ...current, receiverName: event.target.value }))} required placeholder={user?.email || "Ten nguoi nhan"} />
            </Field>
            <Field label="So dien thoai nhan hang">
              <Input value={form.phoneNumber} onChange={(event) => setForm((current) => ({ ...current, phoneNumber: event.target.value }))} required placeholder={user?.phone || "090..."} />
            </Field>
            <Field label="Dia chi chi tiet">
              <Input value={form.detailAddress} onChange={(event) => setForm((current) => ({ ...current, detailAddress: event.target.value }))} required />
            </Field>
            <Field label="Tinh / thanh">
              <Select
                value={form.provinceId}
                onChange={(event) => setForm((current) => ({ ...current, provinceId: event.target.value, districtId: "", wardCode: "" }))}
                required
                disabled={provinces.isLoading || provinces.isError}
              >
                <option value="">{provinces.isLoading ? "Loading provinces..." : "Select province"}</option>
                {provinces.data?.map((province) => <option value={province.ProvinceID} key={province.ProvinceID}>{province.ProvinceName}</option>)}
              </Select>
            </Field>
            <Field label="Quan / huyen">
              <Select
                value={form.districtId}
                onChange={(event) => setForm((current) => ({ ...current, districtId: event.target.value, wardCode: "" }))}
                required
                disabled={!form.provinceId || districts.isLoading || districts.isError}
              >
                <option value="">{districts.isLoading ? "Loading districts..." : "Select district"}</option>
                {districts.data?.map((district) => <option value={district.DistrictID} key={district.DistrictID}>{district.DistrictName}</option>)}
              </Select>
            </Field>
            <Field label="Phuong / xa">
              <Select value={form.wardCode} onChange={(event) => setForm((current) => ({ ...current, wardCode: event.target.value }))} required disabled={!form.districtId || wards.isLoading || wards.isError}>
                <option value="">{wards.isLoading ? "Loading wards..." : "Select ward"}</option>
                {wards.data?.map((ward) => <option value={ward.WardCode} key={ward.WardCode}>{ward.WardName}</option>)}
              </Select>
            </Field>
            <label className="check-row">
              <span>Dat lam dia chi mac dinh</span>
              <input type="checkbox" checked={form.isDefault} onChange={(event) => setForm((current) => ({ ...current, isDefault: event.target.checked }))} />
            </label>
            <div className="button-row">
              <Button loading={save.isPending} type="submit">{editing ? "Cap nhat" : "Them dia chi"}</Button>
              {editing ? <Button type="button" variant="secondary" onClick={() => { setEditing(null); setForm(emptyForm); }}>Huy</Button> : null}
            </div>
          </form>
        </Panel>
        <Panel>
          <h2>Dia chi da luu</h2>
          {addresses.isError ? <EmptyState title="Khong lay duoc dia chi" detail={getApiError(addresses.error)} /> : null}
          {addresses.data?.length === 0 ? <EmptyState title="Chua co dia chi" /> : null}
          <div className="stack">
            {addresses.data?.map((address) => (
              <div className="selection-summary" key={address.id}>
                <div className="row-between">
                  <strong>{address.receiverName || "Chua co ten nguoi nhan"}</strong>
                  {address.isDefault ? <StatusBadge value="DEFAULT" /> : null}
                </div>
                <span>{address.phoneNumber || "Chua co SDT"}</span>
                <span>{address.fullAddress || [address.detailAddress, address.wardName, address.districtName, address.provinceName].filter(Boolean).join(", ")}</span>
                <div className="button-row">
                  <Button type="button" variant="secondary" onClick={() => setEditing(address)}><Edit3 size={15} /> Sua</Button>
                  {!address.isDefault ? <Button type="button" variant="ghost" loading={setDefault.isPending} onClick={() => setDefault.mutate(address.id)}>Mac dinh</Button> : null}
                  <Button type="button" variant="danger" loading={removeAddress.isPending} onClick={() => removeAddress.mutate(address.id)}><Trash2 size={15} /> Xoa</Button>
                </div>
              </div>
            ))}
          </div>
        </Panel>
      </div>
    </div>
  );
}
