export const BULLSEYE_APPLICATION = "bullseye";

export const BULLSEYE_APPEND_ACROSS_OPTIONS = [
  { label: "Jenkins Job Name", value: "job_name" },
  { label: "Jenkins Job Path", value: "job_normalized_full_name" }
];

export const defaultSorts: { [x: string]: any[] } = {
  function: [
    {
      id: "functions_covered",
      desc: true
    }
  ],
  branch: [
    {
      id: "conditions_covered",
      desc: true
    }
  ],
  decision: [
    {
      id: "decisions_covered",
      desc: true
    }
  ],
  code: [
    {
      id: "total_functions",
      desc: true
    },
    {
      id: "total_decisions",
      desc: true
    },
    {
      id: "total_conditions",
      desc: true
    }
  ]
};
