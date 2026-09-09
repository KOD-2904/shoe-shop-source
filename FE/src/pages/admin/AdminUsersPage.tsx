import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { authApi } from "../../api/authApi";
import { getApiError } from "../../api/client";
import { DataTable, EmptyState, PageHeader, Panel, Select, StatusBadge } from "../../components/ui";

export function AdminUsersPage() {
  const [query, setQuery] = useState("");
  const [role, setRole] = useState("ALL");
  const users = useQuery({ queryKey: ["users"], queryFn: authApi.users });
  const filteredUsers = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (users.data ?? []).filter((user) => {
      const matchesQuery = !keyword || [user.email, user.phone].some((value) => value?.toLowerCase().includes(keyword));
      const matchesRole = role === "ALL" || user.roles.includes(role);
      return matchesQuery && matchesRole;
    });
  }, [query, role, users.data]);
  const adminCount = users.data?.filter((user) => user.roles.includes("ROLE_ADMIN")).length ?? 0;

  return (
    <div className="admin-page">
      <PageHeader title="Users" />
      <div className="metric-grid compact">
        <Panel><span>Total users</span><strong>{users.data?.length ?? 0}</strong></Panel>
        <Panel><span>Admins</span><strong>{adminCount}</strong></Panel>
        <Panel><span>Visible</span><strong>{filteredUsers.length}</strong></Panel>
      </div>
      <div className="admin-toolbar">
        <div className="admin-search">
          <Search size={17} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search email or phone" />
        </div>
        <Select value={role} onChange={(event) => setRole(event.target.value)}>
          <option value="ALL">All roles</option>
          <option value="ROLE_ADMIN">Admin</option>
          <option value="ROLE_USER">User</option>
        </Select>
      </div>
      {users.isError ? <EmptyState title="Khong lay duoc users" detail={getApiError(users.error)} /> : null}
      <DataTable>
        <thead><tr><th>Email</th><th>Phone</th><th>Status</th><th>Verified</th><th>Roles</th></tr></thead>
        <tbody>
          {filteredUsers.map((user) => (
            <tr key={user.id}>
              <td><strong>{user.email}</strong></td>
              <td>{user.phone || "-"}</td>
              <td><StatusBadge value={user.status} /></td>
              <td><StatusBadge value={user.emailVerified ? "YES" : "NO"} /></td>
              <td>{user.roles.join(", ")}</td>
            </tr>
          ))}
        </tbody>
      </DataTable>
    </div>
  );
}
