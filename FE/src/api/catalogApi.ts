import { api, unwrap } from "./client";
import type { ApiResponse, BrandResponse, CategoryResponse, ProductRequest, ProductResponse, ProductVariantRequest, ProductVariantResponse, VariantFormOptions } from "../types";

export const catalogApi = {
  products: async () => {
    const { data } = await api.get<ApiResponse<ProductResponse[]> | ProductResponse[]>("/products/getProducts");
    return unwrap<ProductResponse[]>(data);
  },
  product: async (productId: string) => {
    const { data } = await api.get<ApiResponse<ProductResponse> | ProductResponse>("/products/getProduct", { params: { productId } });
    return unwrap<ProductResponse>(data);
  },
  createProduct: async (product: ProductRequest) => {
    const { data } = await api.post<ApiResponse<ProductResponse> | ProductResponse>("/products/", product);
    return unwrap<ProductResponse>(data);
  },
  createProductWithImages: async (product: ProductRequest, images: File[], primaryIndex?: number) => {
    const formData = new FormData();
    formData.append("product", new Blob([JSON.stringify(product)], { type: "application/json" }));
    images.forEach((image) => formData.append("images", image));
    if (primaryIndex !== undefined) formData.append("primaryIndex", String(primaryIndex));
    const { data } = await api.post<ApiResponse<ProductResponse> | ProductResponse>("/products/with-images", formData);
    return unwrap<ProductResponse>(data);
  },
  brands: async () => {
    const { data } = await api.get<ApiResponse<BrandResponse[]> | BrandResponse[]>("/brand/getBrands");
    return unwrap<BrandResponse[]>(data);
  },
  createBrand: async (body: { name: string; logoUrl?: string }) => {
    const { data } = await api.post<ApiResponse<BrandResponse> | BrandResponse>("/brand/add", body);
    return unwrap<BrandResponse>(data);
  },
  createBrandWithImage: async (brand: { name: string; logoUrl?: string }, image?: File) => {
    const formData = new FormData();
    formData.append("brand", new Blob([JSON.stringify(brand)], { type: "application/json" }));
    if (image) formData.append("image", image);
    const { data } = await api.post<ApiResponse<BrandResponse> | BrandResponse>("/brand/addBrandWithImage", formData);
    return unwrap<BrandResponse>(data);
  },
  categories: async () => {
    const { data } = await api.get<ApiResponse<CategoryResponse[]> | CategoryResponse[]>("/category/getCategorys");
    return unwrap<CategoryResponse[]>(data);
  },
  createCategory: async (body: { name: string; parentId?: string | null }) => {
    const { data } = await api.post<ApiResponse<CategoryResponse> | CategoryResponse>("/category/add", body);
    return unwrap<CategoryResponse>(data);
  },
  variantFormOptions: async () => {
    const { data } = await api.get<ApiResponse<VariantFormOptions> | VariantFormOptions>("/variants/form-options");
    return unwrap<VariantFormOptions>(data);
  },
  createVariant: async (variant: ProductVariantRequest) => {
    const { data } = await api.post<ApiResponse<ProductVariantResponse> | ProductVariantResponse>("/variants/addVariant", variant);
    return unwrap<ProductVariantResponse>(data);
  },
  createVariants: async (variants: ProductVariantRequest[]) => {
    const { data } = await api.post<ApiResponse<ProductVariantResponse[]> | ProductVariantResponse[]>("/variants/addVariants", variants);
    return unwrap<ProductVariantResponse[]>(data);
  },
  variants: async () => {
    const { data } = await api.get<ApiResponse<ProductVariantResponse[]> | ProductVariantResponse[]>("/variants");
    return unwrap<ProductVariantResponse[]>(data);
  },
  variantsByProduct: async (productId: string) => {
    const { data } = await api.get<ApiResponse<ProductVariantResponse[]> | ProductVariantResponse[]>(`/variants/product/${productId}`);
    return unwrap<ProductVariantResponse[]>(data);
  },
  addVariantImages: async (variantId: string, images: File[], primaryIndex?: number) => {
    const formData = new FormData();
    images.forEach((image) => formData.append("images", image));
    if (primaryIndex !== undefined) formData.append("primaryIndex", String(primaryIndex));
    const { data } = await api.post<ApiResponse<ProductVariantResponse> | ProductVariantResponse>(`/variants/${variantId}/images`, formData);
    return unwrap<ProductVariantResponse>(data);
  }
};
