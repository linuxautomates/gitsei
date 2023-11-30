import * as helperFunctions from "./helper";
import { isArray } from "lodash";

describe("nodeInputTransformer test suite", () => {
  test("it should return correct response when use_input_fields in fields of input object is false || undefined", () => {
    const mock_data = {
      node: {
        input: {
          columns_to_diff: {
            hidden: false,
            index: 30,
            key: "columns_to_diff",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: undefined
          },
          key_column: {
            hidden: false,
            index: 20,
            key: "key_column",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: false
          },
          plugin_result_id: {
            hidden: false,
            index: 10,
            key: "plugin_result_id",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" }
          }
        }
      },
      predicates: [],
      values: {
        columns_to_diff: {
          value: []
        },
        key_column: {
          value: []
        },
        plugin_result_id: {
          value: []
        }
      }
    };

    const mock_response = [
      {
        content_schema: undefined,
        filters: [],
        label: "columns_to_diff",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      },
      {
        content_schema: undefined,
        filters: [],
        label: "key_column",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      },
      {
        content_schema: undefined,
        filters: [],
        label: "plugin_result_id",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      }
    ];

    expect("node" in mock_data).toBeTruthy();
    expect("input" in mock_data?.node).toBeTruthy();

    expect(helperFunctions.nodeInputTransformer(mock_data)).toStrictEqual(mock_response);
  });

  test("it should return correct response when use_input_fields in fields of input object is false || undefined and 'array' field is present", () => {
    const mock_data = {
      node: {
        input: {
          columns_to_diff: {
            hidden: false,
            index: 30,
            key: "columns_to_diff",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: undefined
          },
          key_column: {
            hidden: false,
            index: 20,
            key: "key_column",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: false
          },
          array: {
            hidden: false,
            index: 10,
            key: "plugin_result_id",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" }
          }
        }
      },
      predicates: [],
      values: {
        columns_to_diff: {
          value: []
        },
        key_column: {
          value: []
        },
        array: {
          value: []
        }
      }
    };

    const mock_response = [
      {
        content_schema: undefined,
        filters: [],
        label: "columns_to_diff",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      },
      {
        content_schema: undefined,
        filters: [],
        label: "key_column",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      },
      {
        content_schema: undefined,
        filters: [],
        label: "array",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "single-variable-select",
        values: {
          value: []
        }
      }
    ];

    expect("node" in mock_data).toBeTruthy();
    expect("input" in mock_data?.node).toBeTruthy();

    expect(helperFunctions.nodeInputTransformer(mock_data)).toStrictEqual(mock_response);
  });

  test("it should return correct response when use_input_fields in fields of input object is true", () => {
    const mock_data = {
      node: {
        input: {
          columns_to_diff: {
            hidden: false,
            index: 30,
            key: "columns_to_diff",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: true
          },
          key_column: {
            hidden: false,
            index: 20,
            key: "key_column",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: true
          },
          plugin_result_id: {
            hidden: false,
            index: 10,
            key: "plugin_result_id",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: true
          }
        }
      },
      predicates: [],
      values: {
        columns_to_diff: {
          value: []
        },
        key_column: {
          value: []
        },
        plugin_result_id: {
          value: []
        }
      }
    };

    const mock_response = [
      {
        content_schema: undefined,
        field_values: {},
        filters: [],
        label: "columns_to_diff",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      },
      {
        content_schema: undefined,
        field_values: {},
        filters: [],
        label: "key_column",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      },
      {
        content_schema: undefined,
        field_values: {},
        filters: [],
        label: "plugin_result_id",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      }
    ];

    expect("node" in mock_data).toBeTruthy();
    expect("input" in mock_data?.node).toBeTruthy();

    expect(helperFunctions.nodeInputTransformer(mock_data)).toStrictEqual(mock_response);
  });

  test("it should return correct response when use_input_fields in fields of input object is true and 'array' field is present", () => {
    const mock_data = {
      node: {
        input: {
          columns_to_diff: {
            hidden: false,
            index: 30,
            key: "columns_to_diff",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: true
          },
          key_column: {
            hidden: false,
            index: 20,
            key: "key_column",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: true
          },
          array: {
            hidden: false,
            index: 10,
            key: "plugin_result_id",
            required: true,
            search_field: undefined,
            type: "text",
            json: { type: "text" },
            use_input_fields: true
          }
        }
      },
      predicates: [],
      values: {
        columns_to_diff: {
          value: []
        },
        key_column: {
          value: []
        },
        array: {
          value: []
        }
      }
    };

    const mock_response = [
      {
        content_schema: undefined,
        field_values: {},
        filters: [],
        label: "columns_to_diff",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      },
      {
        content_schema: undefined,
        field_values: {},
        filters: [],
        label: "key_column",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "autosuggest",
        values: {
          value: []
        }
      },
      {
        content_schema: undefined,
        field_values: {},
        filters: [],
        label: "array",
        predicates: [],
        suggestions: [],
        text_type: "text",
        type: "single-variable-select",
        values: {
          value: []
        }
      }
    ];

    expect("node" in mock_data).toBeTruthy();
    expect("input" in mock_data?.node).toBeTruthy();

    expect(helperFunctions.nodeInputTransformer(mock_data)).toStrictEqual(mock_response);
  });
});
