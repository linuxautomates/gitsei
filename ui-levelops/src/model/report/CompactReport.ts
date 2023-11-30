import { IS_FRONTEND_REPORT } from "../../dashboard/constants/filter-key.mapping";

class CompactReport {
  key: string;
  name: string;
  report_type: string; // same as key.
  supported_widget_types: string[];
  applications: string[];
  categories: string[];
  content?: string;
  imageUrl?: string;
  supported_by_integration?: boolean;
  description: string;
  hide_learn_more_button?: boolean;
  [IS_FRONTEND_REPORT]?: boolean;
}

export default CompactReport;
