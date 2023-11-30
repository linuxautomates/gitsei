export interface GetDataError<TError> {
  message: string;
  data: TError | string;
  status?: number;
}
