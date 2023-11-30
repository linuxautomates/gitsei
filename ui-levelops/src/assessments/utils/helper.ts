import { filter, forEach, get, isArray, map, sortBy } from "lodash";
import moment from "moment";

export enum QuestionsHandlerType {
  CLOSE_PREVIEWER = "closePreviewer",
  PREVIEW_FILE = "previewFile",
  GET_ASSERTION = "getAssertion",
  FILE_UPLOAD = "fileUpload",
  FILE_REMOVE = "fileRemove",
  REMOVE_FILE = "removeFile",
  SHOW_COMMENTS = "showComments",
  UPLOAD_LINK = "uploadLink",
  DOWNLOAD_FILE = "downloadFile",
  UPDATE_MULTI_SELECT = "updateMultiSelect",
  UPDATE_CHECKLIST = "updateChecklist",
  UPDATE_SINGLE_SELECT = "updateSingleSelect",
  UPDATE_TEXT_VALUE = "updateTextValue",
  UPDATE_OPTION = "updateOption",
  UPDATE_COMMENTS = "updateComments",
  UPDATE_QUIZ = "updateQuiz",
  SEND_BY_SLACK = "sendBySlack",
  ANSWER_QUIZ = "answerQuiz"
}

export const filterResponses = (data: any, type: string) => {
  return type === "link" ? filter(data, (f: any) => f.type === "link") : filter(data, (f: any) => f.type !== "link");
};

export const assertionSort = (data: any) => {
  return sortBy(data, [(cur: any) => cur.number]);
};

export const extractAnswer = (answer: any) => {
  let userEmail = "";
  let createdOn = null;
  let comments = [];
  if (answer) {
    comments = answer.comments ? answer.comments : [];
    userEmail = answer.answered ? answer.user_email : "";
    createdOn = answer.created_at ? moment.unix(answer.created_at).format("MMM DD, YYYY") : null;
  }
  return { userEmail, createdOn, comments };
};

export const checkShowComments = (
  showComments: any,
  sectionId: string,
  questionId: string,
  expandComments: boolean
): boolean => {
  return (
    (showComments && showComments.section_id === sectionId && showComments.question_id === questionId) || expandComments
  );
};

export const getCheckListValues = (answer: any) => {
  return answer
    ? answer.responses.map((resp: any) =>
        JSON.stringify({
          value: resp.value !== "" ? true : "",
          score: resp.score
        })
      )
    : [];
};

export const getMultiselectValues = (answer: any) => {
  return answer
    ? answer.responses.map((resp: any) =>
        JSON.stringify({
          value: resp.value,
          score: resp.score,
          original_value: resp.original_value || resp.value
        })
      )
    : [];
};

export const multiselectValue = (option: any, answer: any) => {
  return option.updatedValue !== undefined
    ? option.updatedValue
    : answer.responses.filter((resp: any) => resp.original_value === option.value).length > 0
    ? answer.responses.filter((resp: any) => resp.original_value === option.value)[0].value
    : option.value;
};

export const singleSelectOriginalValue = (singleValue: any, answer: any) => {
  return singleValue
    ? get(answer, ["responses", 0, "original_value"], undefined) || get(answer, ["responses", 0, "value"], undefined)
    : undefined;
};

export const singleSelectValue = (option: any, singleValue: any, originalValue: any) => {
  return option.updatedValue !== undefined
    ? option.updatedValue
    : singleValue && originalValue === option.value
    ? singleValue
    : option.value;
};

export const extractResponse = (response: any) => {
  return { fileId: response.value, fileName: response.file_name, uploadNeeded: response.upload };
};

export const getPrevCreatedAt = (
  answer: any,
  questionId: string,
  updatedValues: any,
  answered: boolean,
  userEmail: string | null
) => {
  let prevCreatedAt = undefined;
  forEach(answer.answers, (assert: any) => {
    console.log(assert.question_id, questionId);
    if (assert.question_id === questionId) {
      prevCreatedAt = assert.created_at;
      assert.responses = updatedValues;
      assert.user_email = userEmail;
      assert.created_at = moment().unix();
      assert.answered = answered;
    }
  });
  return { prevCreatedAt, newAnswer: answer };
};

export const getResponse = (file: any, responseLength: number, score: string, link: boolean) => {
  return file !== undefined
    ? {
        value: file,
        file_name: file.name,
        score: responseLength === 0 ? parseInt(score) : 0,
        type: link === false ? "file" : "link",
        upload: file !== undefined && link === false ? true : undefined
      }
    : {
        value: "",
        file_name: "",
        type: "file",
        score: 0,
        upload: false
      };
};

export const modifyQuiz = (quiz: any, id: string, answer: any) => {
  let curQuiz = quiz;
  forEach(curQuiz?.section_responses, (ans: any) => {
    if (ans.section_id === id) {
      ans.answers = answer.answers;
    }
  });
  return curQuiz;
};

export const getCalculatedRiskScore = (responses: any) => {
  let total_score = 0;
  let totalAnswered = 0;
  forEach(responses, answer => {
    forEach(answer.answers, assertion => {
      if (assertion.answered) {
        totalAnswered = totalAnswered + 1;
        if (isArray(assertion.responses)) {
          forEach(assertion.responses, res => {
            total_score = total_score + parseInt(res.score);
          });
        } else {
          total_score = total_score + parseInt(assertion.responses.score);
        }
      }
    });
  });
  return { score: total_score, answered: totalAnswered };
};

export const getAssertion = (responses: any, questionId: any, assertionId: any) => {
  for (let i = 0; i < responses.length; i++) {
    if (responses[i].section_id.toString() === questionId.toString()) {
      let answer = responses[i];
      for (let j = 0; j < answer.answers.length; j++) {
        if (answer.answers[j].question_id.toString() === assertionId.toString()) {
          return answer.answers[j];
        }
      }
    }
  }
  return null;
};

interface QnAProps {
  answered_questions?: number | null;
  total_questions?: number | null;
}

export function getQnAProgress(props: QnAProps) {
  if (
    props.answered_questions === null ||
    props.answered_questions === undefined ||
    props.total_questions === null ||
    props.total_questions === undefined
  ) {
    return 0;
  }

  // handling cases where total_questions are none hence progress is 100%
  if (props.total_questions === 0) return 100;
  return Math.ceil((props.answered_questions / props.total_questions) * 100);
}

export const mapAllFiles = (quiz: any) => {
  let initialFiles: any = {};
  forEach(quiz?.section_responses, answer => {
    forEach(answer.answers, assertion => {
      if (assertion.answered && assertion.responses !== undefined && assertion.responses[0] !== undefined) {
        initialFiles = {
          ...initialFiles,
          [answer.section_id]: {
            ...(initialFiles?.[answer.section_id] || {}),
            [assertion.question_id]: map(assertion.responses, (res, index) => ({
              ...res,
              upload_id: `${quiz?.id}:${assertion.question_id}:${index}`
            }))
          }
        };
      }
    });
  });
  return initialFiles;
};
