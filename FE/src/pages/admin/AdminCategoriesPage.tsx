import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FolderPlus, Search } from "lucide-react";
import { catalogApi } from "../../api/catalogApi";
import { getApiError } from "../../api/client";
import { Button, DataTable, EmptyState, Field, Input, PageHeader, Panel, Select, StatusBadge } from "../../components/ui";
import { cacheTimes } from "../../lib/queryCache";
import { useToast } from "../../state/ToastContext";

export function AdminCategoriesPage() {
  const [name, setName] = useState("");
  const [parentId, setParentId] = useState("");
  const [query, setQuery] = useState("");
  const [parentFilter, setParentFilter] = useState("ALL");
  const toast = useToast();
  const queryClient = useQueryClient();
  const categories = useQuery({ queryKey: ["categories"], queryFn: catalogApi.categories, ...cacheTimes.catalogReference });
  const categoryById = useMemo(() => new Map((categories.data ?? []).map((category) => [category.id, category.name])), [categories.data]);
  const filteredCategories = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (categories.data ?? []).filter((category) => {
      const parentName = category.parentId ? categoryById.get(category.parentId) : "";
      const matchesQuery = !keyword || [category.name, parentName].some((value) => value?.toLowerCase().includes(keyword));
      const matchesParent =
        parentFilter === "ALL" ||
        (parentFilter === "ROOT" ? !category.parentId : parentFilter === "CHILD" ? Boolean(category.parentId) : category.parentId === parentFilter);
      return matchesQuery && matchesParent;
    });
  }, [categories.data, categoryById, parentFilter, query]);
  const create = useMutation({
    mutationFn: () => catalogApi.createCategory({ name, parentId: parentId || null }),
    onSuccess: async () => {
      toast.success("Da tao category");
      setName("");
      setParentId("");
      await queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const submit = (event: FormEvent) => { event.preventDefault(); create.mutate(); };

  return (
    <div className="admin-page">
      <PageHeader title="Categories" />
      <Panel className="admin-main-panel">
        <div className="admin-toolbar admin-toolbar-wrap">
          <div className="admin-search">
            <Search size={17} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search category or parent" />
          </div>
          <Select value={parentFilter} onChange={(event) => setParentFilter(event.target.value)}>
            <option value="ALL">All categories</option>
            <option value="ROOT">Root categories</option>
            <option value="CHILD">Child categories</option>
            {categories.data?.filter((category) => !category.parentId).map((category) => (
              <option value={category.id} key={category.id}>Under {category.name}</option>
            ))}
          </Select>
          <span className="toolbar-count">{filteredCategories.length} categories</span>
        </div>
        {categories.isError ? <EmptyState title="Khong lay duoc categories" detail={getApiError(categories.error)} /> : null}
        <DataTable>
          <thead><tr><th>Category</th><th>Parent</th><th>Type</th></tr></thead>
          <tbody>
            {filteredCategories.map((category) => (
              <tr key={category.id}>
                <td><strong>{category.name}</strong></td>
                <td>{category.parentId ? categoryById.get(category.parentId) || "Unknown parent" : "-"}</td>
                <td><StatusBadge value={category.parentId ? "CHILD" : "ROOT"} /></td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      </Panel>
      <Panel>
        <h2><FolderPlus size={17} /> Create category</h2>
        <form className="form-grid admin-create-form" onSubmit={submit}>
          <Field label="Name"><Input value={name} onChange={(event) => setName(event.target.value)} required /></Field>
          <Field label="Parent category">
            <Select value={parentId} onChange={(event) => setParentId(event.target.value)} disabled={categories.isLoading || categories.isError}>
              <option value="">{categories.isLoading ? "Loading categories..." : "No parent category"}</option>
              {categories.data?.map((category) => (
                <option value={category.id} key={category.id}>{category.name}</option>
              ))}
            </Select>
          </Field>
          <Button loading={create.isPending} type="submit">Create</Button>
        </form>
      </Panel>
    </div>
  );
}
