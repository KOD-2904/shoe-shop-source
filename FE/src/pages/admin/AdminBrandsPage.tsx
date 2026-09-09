import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ImagePlus, Search, Tags } from "lucide-react";
import { catalogApi } from "../../api/catalogApi";
import { getApiError } from "../../api/client";
import { Button, DataTable, EmptyState, Field, Input, PageHeader, Panel, Select } from "../../components/ui";
import { cacheTimes } from "../../lib/queryCache";
import { useToast } from "../../state/ToastContext";

function BrandLogo({ src, name }: { src?: string; name: string }) {
  const [failed, setFailed] = useState(false);

  return (
    <div className="brand-logo-cell">
      {src && !failed ? (
        <img src={src} alt={`${name} logo`} onError={() => setFailed(true)} />
      ) : (
        <div className="brand-logo-fallback"><Tags size={18} /></div>
      )}
    </div>
  );
}

export function AdminBrandsPage() {
  const [name, setName] = useState("");
  const [logoUrl, setLogoUrl] = useState("");
  const [image, setImage] = useState<File | undefined>();
  const [logoMode, setLogoMode] = useState<"upload" | "url">("upload");
  const [query, setQuery] = useState("");
  const [logoFilter, setLogoFilter] = useState("ALL");
  const toast = useToast();
  const queryClient = useQueryClient();
  const brands = useQuery({ queryKey: ["brands"], queryFn: catalogApi.brands, ...cacheTimes.catalogReference });
  const filteredBrands = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (brands.data ?? []).filter((brand) => {
      const matchesQuery = !keyword || brand.name.toLowerCase().includes(keyword);
      const matchesLogo = logoFilter === "ALL" || (logoFilter === "WITH_LOGO" ? Boolean(brand.logoUrl) : !brand.logoUrl);
      return matchesQuery && matchesLogo;
    });
  }, [brands.data, logoFilter, query]);
  const create = useMutation({
    mutationFn: () => {
      if (logoMode === "upload" && image) return catalogApi.createBrandWithImage({ name }, image);
      return catalogApi.createBrand({ name, logoUrl: logoMode === "url" ? logoUrl : undefined });
    },
    onSuccess: async () => {
      toast.success("Da tao brand");
      setName("");
      setLogoUrl("");
      setImage(undefined);
      await queryClient.invalidateQueries({ queryKey: ["brands"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const submit = (event: FormEvent) => { event.preventDefault(); create.mutate(); };

  return (
    <div className="admin-page">
      <PageHeader title="Brands" />
      <Panel className="admin-main-panel">
        <div className="admin-toolbar admin-toolbar-wrap">
          <div className="admin-search">
            <Search size={17} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search brand" />
          </div>
          <Select value={logoFilter} onChange={(event) => setLogoFilter(event.target.value)}>
            <option value="ALL">All logos</option>
            <option value="WITH_LOGO">Has logo</option>
            <option value="NO_LOGO">Missing logo</option>
          </Select>
          <span className="toolbar-count">{filteredBrands.length} brands</span>
        </div>
        {brands.isError ? <EmptyState title="Khong lay duoc brands" detail={getApiError(brands.error)} /> : null}
        <DataTable>
          <thead><tr><th>Logo</th><th>Brand</th><th>Logo source</th></tr></thead>
          <tbody>
            {filteredBrands.map((brand) => (
              <tr key={brand.id}>
                <td><BrandLogo src={brand.logoUrl} name={brand.name} /></td>
                <td><strong>{brand.name}</strong></td>
                <td className="table-subtext">{brand.logoUrl || "-"}</td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      </Panel>
      <Panel>
        <h2><ImagePlus size={17} /> Create brand</h2>
        <form className="form-grid admin-create-form" onSubmit={submit}>
          <Field label="Name"><Input value={name} onChange={(event) => setName(event.target.value)} required /></Field>
          <Field label="Logo source">
            <Select value={logoMode} onChange={(event) => setLogoMode(event.target.value as "upload" | "url")}>
              <option value="upload">Upload file to Cloudinary</option>
              <option value="url">Use existing URL</option>
            </Select>
          </Field>
          {logoMode === "url" ? (
            <Field label="Logo URL"><Input value={logoUrl} onChange={(event) => setLogoUrl(event.target.value)} /></Field>
          ) : (
            <Field label="Logo file"><Input type="file" accept="image/*" onChange={(event) => setImage(event.target.files?.[0])} /></Field>
          )}
          {logoMode === "url" && logoUrl ? <BrandLogo src={logoUrl} name={name || "Brand"} /> : null}
          <Button loading={create.isPending} type="submit">Create</Button>
        </form>
      </Panel>
    </div>
  );
}
