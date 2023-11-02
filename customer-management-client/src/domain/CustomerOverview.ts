import type { Customer } from './Customer';

export type CustomerOverview = Customer & {
  self: { href: string };
};
