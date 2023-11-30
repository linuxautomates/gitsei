import { RestQuestion } from "classes/RestQuestionnaire";
import { uniq } from "lodash";

export class AssessmentService {
  static REQUIRED_COLUMNS = [
    "Template Name",
    "Tags",
    "Section Name",
    "Section Description",
    "Question",
    "Required",
    "Type",
    "Severity"
    //"Knowledge Base"
  ];

  static DEFAULT_HEADER_MAPPING = {
    "Template Name": "templateName",
    Tags: "tagNames",
    "Section Name": "sectionName",
    "Section Description": "sectionDescription",
    Question: "questionName",
    Required: "requiredName",
    Type: "typeName",
    severity: "severityName",
    "Knowledge Base": "knowledgeBase"
  };

  /**
   *
   * @param assessment
   * @returns {{data: *[][], columns: (string)[]}}
   */
  static convertAssessmentIntoExportFormat(assessment) {
    const { name, sections, tags } = assessment;

    let headers = [
      { label: "Template Name", key: "template_name" },
      { label: "Tags", key: "tags" },
      { label: "Section Name", key: "section_id" },
      { label: "Section Description", key: "section_description" },
      { label: "Question", key: "question" },
      { label: "Required", key: "required" },
      { label: "Type", key: "type" },
      { label: "Severity", key: "severity" },
      { label: "Knowledge Base", key: "kbs" }
    ];
    const data = [];
    let maxOptionsCount = 0;
    // eslint-disable-next-line array-callback-return
    sections.map(section => {
      // eslint-disable-next-line array-callback-return
      section.questions.map((question, index) => {
        const row = {};
        row["section_id"] = section.name;
        row["section_description"] = section.description;
        row["question"] = question.name;
        row["type"] = question.type;
        row["required"] = !!question.required ? "Yes" : "No";
        row["severity"] = question.severity;
        if (maxOptionsCount < question.options.length) {
          maxOptionsCount = question.options.length;
        }
        // eslint-disable-next-line array-callback-return
        question.options.map((option, index) => {
          row[`option_${index}_value`] = option.value;
          row[`option_${index}_score`] = option.score;
        });
        data.push(row);
      });
    });

    data.forEach(row => (row["template_name"] = name));
    data.forEach(row => (row["tags"] = tags.map(tag => tag.name).join()));
    //data[0]["kbs"] = kbs.map(kb => kb.name).join();

    for (let i = 0; i < maxOptionsCount; i++) {
      headers.push({
        label: `Value`,
        key: `option_${i}_value`
      });
      headers.push({
        label: `Score`,
        key: `option_${i}_score`
      });
    }

    // headers.push({
    //   label: `Knowledge Base`,
    //   key: `kbs`
    // });

    const rawData = [
      headers.map(header => header.label),
      ...data.map(data => {
        return headers.map(header => data[header.key]);
      })
    ];

    return rawData
      .map(row =>
        row
          .map(value =>
            value || value === 0 ? (typeof value === "string" ? `"${value.replace(/"/g, '""')}"` : `"${value}"`) : ""
          )
          .join(",")
      )
      .join("\n");
  }

  /**
   *
   * @param resp
   * @returns {[{name: string, sections: [], tags: []}]}
   */
  static parseExcelDataIntoReadableFormat = (resp, colDetails, templateNames) => {
    const {
      rows: [header, ...rows]
    } = resp;

    const indexOfAll = (arr, val) => arr.reduce((acc, el, i) => (el === val ? [...acc, i] : acc), []);

    const tagIndexs = colDetails.tagNames
      ? colDetails.tagNames.map(tagName => header.findIndex(col => col === tagName))
      : [];
    const sectionIndex = header.findIndex(col => col === colDetails.sectionName);
    const kbsIndex = header.findIndex(col => col === colDetails.knowledgeBase);
    const sectionDescIndex = header.findIndex(col => col === colDetails.sectionDescription);
    const questionIndex = header.findIndex(col => col === colDetails.questionName);
    const requiredIndex = header.findIndex(col => col === colDetails.requiredName);
    const typeIndex = header.findIndex(col => col === colDetails.typeName);
    const severityIndex = header.findIndex(col => col === colDetails.severityName);
    const optionIndices = indexOfAll(header, "Value");
    const scoreIndices = indexOfAll(header, "Score");

    console.log(optionIndices);
    console.log(scoreIndices);

    if (colDetails.templateName) {
      const nameIndex = header.findIndex(col => col === colDetails.templateName);
      const templateNameData = rows.reduce(
        (acc, next) => ({ ...acc, [next[nameIndex]]: [...(acc[next[nameIndex]] || []), next] }),
        {}
      );
      return Object.keys(templateNameData)
        .filter(key => key !== "undefined" && !!key)
        .map(key =>
          AssessmentService.singleTemplate(
            templateNameData[key],
            templateNames.find(item => item.id === key).currentName,
            tagIndexs,
            sectionIndex,
            questionIndex,
            sectionDescIndex,
            requiredIndex,
            typeIndex,
            severityIndex,
            optionIndices,
            scoreIndices,
            kbsIndex
          )
        );
    } else {
      const template = AssessmentService.singleTemplate(
        rows,
        templateNames[0].currentName,
        tagIndexs,
        sectionIndex,
        questionIndex,
        sectionDescIndex,
        requiredIndex,
        typeIndex,
        severityIndex,
        optionIndices,
        scoreIndices
      );
      return [template];
    }
  };

  static singleTemplate = (
    rows,
    tempateName,
    tagIndexs,
    sectionIndex,
    questionIndex,
    sectionDescIndex,
    requiredIndex = -1,
    typeIndex = -1,
    severityIndex = -1,
    optionIndices = [],
    scoreIndices = [],
    kbsIndex
  ) => {
    const dataset = {
      name: tempateName,
      // tags: uniq(
      //   rows
      //     .reduce((acc, row) => [...acc, ...tagIndexs.map(tag => row[tag])], [])
      //     .filter(val => !!val)
      //     .map(val => val.toString().toLowerCase())
      // ),
      tags: [],
      kbs: uniq(rows.reduce((acc, row) => (row[kbsIndex] ? [...acc, ...row[kbsIndex].split(",")] : acc), [])),
      sections: [],
      errorMessage: null
    };

    // let errorMessage = AssessmentService.validateColumns(header);
    // if (errorMessage) {
    // 	dataset.errorMessage = errorMessage;
    // 	return dataset;
    // }

    const sections = [];
    let currentSection = null;
    let currentSectionName = "";

    // Removing empty rows...
    const questions = rows.filter(row => !!row.length);

    // eslint-disable-next-line array-callback-return
    questions.map((row, index) => {
      const _sectionTitle = row[sectionIndex];
      const _sectionDesc = row[sectionDescIndex];

      const _isFirstQuestionOfSection = !!_sectionTitle && _sectionTitle !== "" && _sectionTitle !== currentSectionName;

      if (_isFirstQuestionOfSection) {
        // Creating current Section object.
        currentSection = {
          name: _sectionTitle,
          description: _sectionDesc,
          questions: []
        };
        currentSectionName = _sectionTitle;
        sections.push(currentSection);
      }

      const _question = row[questionIndex];
      const _required = requiredIndex !== -1 ? row[requiredIndex] : "No";
      const _type = typeIndex !== -1 ? row[typeIndex] : "text";
      const _severity = severityIndex !== -1 ? row[severityIndex] : "medium";
      //const _options = []; // Todo implement later

      const _options = optionIndices
        .map(index => row[index])
        .filter(opt => opt && opt !== "")
        .map(opt => ({ value: opt, score: 0 }));

      //let mappedOptions = _mapOptions(_options);
      let mappedOptions = _options;
      if (mappedOptions.length === 0) {
        switch (_type) {
          case "boolean":
            mappedOptions = [
              { value: "yes", score: 0 },
              { value: "no", score: 0 }
            ];
            break;
          case "single-select":
          case "multi-select":
            mappedOptions = [{ value: "default", score: 0 }];
            break;
          default:
            mappedOptions = [{ value: "", score: 0 }];
            break;
        }
      }

      currentSection.questions.push({
        name: _question,
        required: _required ? _required.toLowerCase() === "yes" : false,
        type: _type,
        severity: _severity,
        options: mappedOptions,
        number: index + 1
      });
    });

    dataset.sections = sections;
    // dataset.errorMessage = AssessmentService.validateData(dataset);
    return dataset;
  };

  static validateColumns = columns => {
    for (let i = 0; i < AssessmentService.REQUIRED_COLUMNS.length; i++) {
      if (!columns.includes(AssessmentService.REQUIRED_COLUMNS[i])) {
        return `File doesn't have column: ${AssessmentService.REQUIRED_COLUMNS[i]}`;
      }
    }
    return null;
  };

  static validateData = dataset => {
    const _genericError = "Data Invalid, Can't import.";
    const _allowedSeverities = ["low", "medium", "high"];

    if (!dataset.name) {
      return "Template name is required.";
    }

    if (dataset.sections.length === 0) {
      return "At-least one section is required.";
    }

    for (const section of dataset.sections) {
      console.log("section", section);
      const questionsLen = section.questions.length;

      if (!section.name || section.name === "") {
        console.log(`Section Name is Required.. ${section.name}`);
        // Section Name is Required..
        return _genericError;
      }

      if (questionsLen === 0) {
        console.log(`At-least 1 question per section ${questionsLen}`);
        // At-least 1 question per section
        return _genericError;
      }

      for (const question of section.questions) {
        if (!question.name || question.name === "") {
          console.log(`Question Name is Required.. ${question.name}`);
          // Question Name is Required..
          return _genericError;
        }

        if (!_allowedSeverities.includes(question.severity.toLowerCase())) {
          console.log(`question severity error ${question.severity}`);
          return _genericError;
        }

        if (!RestQuestion.TYPES.includes(question.type.toLowerCase())) {
          console.log(`question type error ${question.type}`);
          return _genericError;
        }

        // console.log(question.name);
        // console.log(question.options);

        for (const option of question.options) {
          if (option.value === undefined || option.value === null) {
            if (option.value === "") {
              console.log("blank option value");
            }
            console.log(`${option.value} value error`);
            return `Import Failed. Invalid Option for question ${question.name} in section ${section.name}`;
            //return _genericError;
          }

          if (typeof option.score !== "number") {
            console.log(`question score error ${option.score}`);
            return `Import Failed. Invalid Score for question ${question.name} in section ${section.name}`;
            //return _genericError;
          }
        }
      }
    }

    return null;
  };

  static columnNameOptions(resp) {
    return resp.rows[0].map(name => ({ label: name, value: name }));
  }

  static initialColumns(resp) {
    let preSelectedColumnNames = {};
    const header = resp.rows[0];
    console.log(header);
    const headerList = header.map(v => !!v && v.toString().toLowerCase());
    Object.keys(AssessmentService.DEFAULT_HEADER_MAPPING).forEach(name => {
      if (headerList.includes(name.toLowerCase())) {
        const index = headerList.findIndex(colName => colName.toLowerCase() === name.toLowerCase());
        let value = header[index];
        if (name.toLowerCase() === "tags") {
          value = [value];
        }
        preSelectedColumnNames = { ...preSelectedColumnNames, [AssessmentService.DEFAULT_HEADER_MAPPING[name]]: value };
      }
    });

    return preSelectedColumnNames;
  }
}
