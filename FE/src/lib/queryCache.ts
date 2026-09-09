const minute = 60 * 1000;

export const cacheTimes = {
  productDetail: {
    staleTime: 5 * minute,
    gcTime: 30 * minute
  },
  catalogReference: {
    staleTime: 30 * minute,
    gcTime: 2 * 60 * minute
  },
  userProfile: {
    staleTime: 2 * minute,
    gcTime: 10 * minute
  },
  shippingLocation: {
    staleTime: 24 * 60 * minute,
    gcTime: 24 * 60 * minute
  }
} as const;

export const queryKeys = {
  profile: ["profile", "me"] as const
} as const;
