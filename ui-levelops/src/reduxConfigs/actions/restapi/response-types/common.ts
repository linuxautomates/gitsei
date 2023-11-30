export interface ListResponse<T> {
  records: T[];
  count: number;
  _metadata: {
    page: number;
    page_size: number;
    has_next: boolean;
    total_count: number;
  };
}
