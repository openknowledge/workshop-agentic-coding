import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchCustomers,
  fetchCustomer,
  updateCustomer,
  deleteCustomer,
  createCustomer,
} from './customerApi';
import type { Customer } from '../../../domain/Customer';

export const customerKeys = {
  all: ['customers'] as const,
  detail: (id: number) => ['customers', id] as const,
};

export const customersQueryOptions = queryOptions({
  queryKey: customerKeys.all,
  queryFn: fetchCustomers,
});

export const customerQueryOptions = (id: number) =>
  queryOptions({
    queryKey: customerKeys.detail(id),
    queryFn: () => fetchCustomer(id),
  });

export function useDeleteCustomer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteCustomer(id),
    onSuccess: async (_, id) => {
      queryClient.removeQueries({ queryKey: customerKeys.detail(id) });
      await queryClient.invalidateQueries({ queryKey: customerKeys.all });
    },
  });
}

export function useUpdateCustomer(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Customer) => updateCustomer(id, data),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: customerKeys.all });
    },
  });
}

export function useCreateCustomer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Customer) => createCustomer(data),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: customerKeys.all });
    },
  });
}
