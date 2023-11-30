export enum SortOptions {
  ASC = "asc",
  DESC = "desc"
}

export const STRING_SORT_OPTIONS = [
  {
    label: "A to Z",
    value: SortOptions.ASC
  },
  {
    label: "Z to A",
    value: SortOptions.DESC
  }
];

export const NUMERIC_SORT_OPTIONS = [
  {
    label: "low to high",
    value: SortOptions.ASC
  },
  {
    label: "high to low",
    value: SortOptions.DESC
  }
];
