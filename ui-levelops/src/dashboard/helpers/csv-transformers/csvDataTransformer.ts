import { genericCsvTransformer } from "./genericCsvTransformer";
import { microsoftCsvTransformer } from "./microsoftCsvTransformer";
import * as RestURI from "constants/restUri";

export const csvDataTransformer = (action: any, columnKey: string, record: any) => {
  switch (action.uri) {
    case RestURI.MICROSOFT_ISSUES: {
      return microsoftCsvTransformer(columnKey, record);
    }
    default: {
      return genericCsvTransformer(columnKey, record);
    }
  }
};
