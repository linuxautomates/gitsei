export const nodeInputTransformer = (data: { node: any; predicates: any; values: any }) => {
  const {
    node: { input },
    predicates,
    values
  } = data;
  let inputArray: any[] = [];
  Object.keys(input || {}).forEach(field => {
    if (input[field].hidden !== true) {
      const inputJson = input[field].json;
      if (inputJson.type === "cron") {
        inputJson.type = "text";
      }
      if ((inputJson.type === "text" || inputJson.type === "text-area") && inputJson.autosuggest !== false) {
        inputJson.text_type = inputJson.type;
        inputJson.type = "autosuggest";
      }
      if (input[field].use_input_fields) {
        const fieldValues = Object.keys(input[field].use_input_fields || {}).reduce((acc: any, obj: any) => {
          acc[obj] = values[input[field].use_input_fields[obj]];
          return acc;
        }, {});
        inputJson.field_values = fieldValues;
      }
      if (field === "array") {
        inputJson.type = "single-variable-select";
      }
      inputJson.label = inputJson.display_name || field;
      inputJson.values = values[field];
      inputJson.content_schema = input[field].content_schema;
      inputJson.predicates = (predicates || []).map((p: { key: any }) => ({
        columnField: p.key,
        type: "text"
      }));
      inputJson.suggestions = predicates;
      inputJson.filters = input[field].filters || [];
      inputArray.push(inputJson);
    }
  });

  return inputArray;
};
