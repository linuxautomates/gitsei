import { mergeCustomHygieneFilters } from "../jiraDrilldownTransformer";
describe("jiraDrilldownTransformer.mergeCustomHygieneFilters tests", () => {
  // test a request from saga, drilldown, open report.
  const test_cases = [
    {
      it: "A merge in hygiene saga request",
      input: {
        firstFilters: {
          id: "0ec274e0-8e68-11eb-a6f5-6536e344e029",
          name: "story point missing",
          filter: { exclude: { custom_fields: {} }, issue_types: ["BUG", "EPIC"], custom_fields: {} },
          missing_fields: { story_points: false, customfield_10020: false }
        },
        secondFilters: {
          filter: {
            projects: ["LFE"],
            custom_fields: {},
            status_categories: ["To Do", "In Progress"],
            exclude: { custom_fields: { customfield_10020: ["FE20210318"] } },
            product_id: "186",
            integration_ids: ["651"]
          },
          across: "project"
        },
        thirdFilters: undefined
      },
      output: {
        id: "0ec274e0-8e68-11eb-a6f5-6536e344e029",
        name: "story point missing",
        filter: {
          exclude: { custom_fields: { customfield_10020: ["FE20210318"] } },
          issue_types: ["BUG", "EPIC"],
          custom_fields: {},
          projects: ["LFE"],
          status_categories: ["To Do", "In Progress"],
          product_id: "186",
          integration_ids: ["651"],
          missing_fields: { story_points: false, customfield_10020: false },
          hygiene_types: []
        },
        missing_fields: { story_points: false, customfield_10020: false }
      }
    },
    {
      it: "A merge to make drilldown request",
      input: {
        firstFilters: {
          filter: {
            projects: ["LFE"],
            custom_fields: {},
            status_categories: ["To Do", "In Progress"],
            exclude: { custom_fields: { customfield_10020: ["FE20210318"] } },
            product_id: "186",
            integration_ids: ["651"]
          },
          across: "hygiene_type"
        },
        secondFilters: {
          hygiene: "story_point_missing",
          id: "0ec274e0-8e68-11eb-a6f5-6536e344e029",
          missing_fields: { story_points: false, customfield_10020: false },
          filter: { exclude: { custom_fields: {} }, issue_types: ["BUG", "EPIC"], custom_fields: {} }
        },
        thirdFilters: {
          filter: {
            exclude: { custom_fields: { customfield_10020: ["FE20210318"] } },
            projects: ["LFE"],
            custom_fields: {},
            status_categories: ["To Do", "In Progress"]
          }
        }
      },
      output: {
        filter: {
          projects: ["LFE"],
          custom_fields: {},
          status_categories: ["To Do", "In Progress"],
          exclude: { custom_fields: { customfield_10020: ["FE20210318"] } },
          product_id: "186",
          integration_ids: ["651"],
          issue_types: ["BUG", "EPIC"],
          missing_fields: { story_points: false, customfield_10020: false },
          hygiene_types: []
        },
        across: "hygiene_type"
      }
    },
    {
      it: "A merge to make open report request",
      input: {
        firstFilters: {
          filter: {
            projects: ["LFE"],
            status_categories: ["To Do", "In Progress"],
            exclude: { custom_fields: { customfield_10020: ["FE20210318"] } },
            product_id: "186",
            integration_ids: ["651"],
            hygiene_types: ["story_point_missing"]
          },
          across: "hygiene_type"
        },
        secondFilters: {
          id: "0ec274e0-8e68-11eb-a6f5-6536e344e029",
          name: "story point missing",
          filter: { exclude: { custom_fields: {} }, issue_types: ["BUG", "EPIC"], custom_fields: {} },
          missing_fields: { story_points: false, customfield_10020: false }
        },
        thirdFilters: undefined
      },
      output: {
        filter: {
          projects: ["LFE"],
          status_categories: ["To Do", "In Progress"],
          exclude: { custom_fields: { customfield_10020: ["FE20210318"] } },
          product_id: "186",
          integration_ids: ["651"],
          hygiene_types: [],
          issue_types: ["BUG", "EPIC"],
          custom_fields: {},
          missing_fields: { story_points: false, customfield_10020: false }
        },
        across: "hygiene_type"
      }
    }
  ];

  test_cases.forEach(test_case => {
    test(test_case.it || "should work", () => {
      //@ts-ignore
      const result = mergeCustomHygieneFilters(
        test_case.input.firstFilters,
        test_case.input.secondFilters,
        test_case.input.thirdFilters
      );
      expect(result).toStrictEqual(test_case.output);
    });
  });
});
