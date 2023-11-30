import * as helperFunctions from "./helper";

describe("assessment helper test suite", () => {
  const mock_data = [
    {
      value: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
      score: 0,
      type: "link"
    },
    {
      value: "http://www.africau.edu/images/default/sample.pdf",
      score: 0,
      type: "link"
    },
    {
      value: "http://www.africau.edu/images/default/sample.pdf",
      score: 0,
      type: "nolink"
    }
  ];
  const comments = ["1", "@", "#"];
  const userEmail = "test@gmail.com";
  const mockQuestionId = "14b24095-6edc-4d21-ade6-171867f8c584";
  const mockAnswer = {
    comments: comments,
    question_id: mockQuestionId,
    user_email: userEmail,
    answered: true,
    upload: false,
    responses: [
      {
        value: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
        score: 0,
        type: "link",
        original_value: "91273hk12398"
      },
      {
        value: "http://www.africau.edu/images/default/sample.pdf",
        score: 0,
        type: "link",
        original_value: "73hk12398"
      }
    ],
    created_at: 1612347634
  };

  const mockResponses = [
    {
      section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
      answers: [
        {
          comments: [],
          question_id: "14b24095-6edc-4d21-ade6-171867f8c584",
          user_email: "hemant.saini@devslane.com",
          answered: true,
          upload: false,
          responses: [
            {
              value: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
              score: 0,
              type: "link"
            },
            {
              value: "http://www.africau.edu/images/default/sample.pdf",
              score: 0,
              type: "link"
            }
          ],
          created_at: 1612347634
        }
      ]
    }
  ];

  const mockQuiz = {
    id: "693f8a41-f767-4ea2-85f0-d799466a9333",
    work_item_id: "8bd2a232-bcd3-4dab-bf69-74a7e07ec486",
    target_email: "thanh@levelops.io",
    sender_email: "thanh@levelops.io",
    assignment_message: "",
    questionnaire_id: "4fcb3146-4062-4aba-8aa8-413982e6b3c6",
    completed: false,
    qtemplate_name: "AC-03-Q-5-1",
    section_responses: [
      {
        section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        answers: [
          {
            comments: [],
            question_id: "14b24095-6edc-4d21-ade6-171867f8c584",
            user_email: "hemant.saini@devslane.com",
            answered: true,
            upload: false,
            responses: [
              {
                value: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                score: 0,
                type: "link"
              },
              {
                value: "http://www.africau.edu/images/default/sample.pdf",
                score: 0,
                type: "link"
              }
            ],
            created_at: 1612347634
          }
        ]
      }
    ],
    sections: [
      {
        tags: [],
        id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        name:
          "Password policy for length, complexity and lifetime is enforced for all access accounts for the environment.",
        type: "DEFAULT",
        description: "testing gfd sgf",
        questions: [
          {
            id: "14b24095-6edc-4d21-ade6-171867f8c584",
            name: "Please provide a configuration screenshot showing LaunchPad is integrated with OneLogin. ",
            severity: "MEDIUM",
            section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
            type: "file upload",
            options: [
              {
                value: "",
                score: 0
              }
            ],
            verifiable: false,
            number: 1,
            required: false,
            tag_ids: []
          }
        ],
        created_at: 1603224688
      }
    ],
    total_questions: 1,
    answered_questions: 1,
    product_id: "71",
    generation: "1612347634979627",
    kb_ids: []
  };

  test("filterResponses should filter correct nolink responses", () => {
    const mock_response = [
      {
        value: "http://www.africau.edu/images/default/sample.pdf",
        score: 0,
        type: "nolink"
      }
    ];

    expect(helperFunctions.filterResponses(mock_data, "nolink")).toStrictEqual(mock_response);
  });

  test("filterResponses should filter correct link responses", () => {
    const mock_response = [
      {
        value: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
        score: 0,
        type: "link"
      },
      {
        value: "http://www.africau.edu/images/default/sample.pdf",
        score: 0,
        type: "link"
      }
    ];

    expect(helperFunctions.filterResponses(mock_data, "link")).toStrictEqual(mock_response);
  });

  test("filterResponses should work with empty array", () => {
    expect(helperFunctions.filterResponses([], "link")).toEqual([]);
  });

  test("filterResponses shouldn't break if data passed is not array", () => {
    expect(helperFunctions.filterResponses(null, "link")).toEqual([]);
    expect(helperFunctions.filterResponses(undefined, "link")).toEqual([]);
    expect(helperFunctions.filterResponses({}, "link")).toEqual([]);
  });

  test("assertionSort shouldn't break if data passed is not array", () => {
    expect(helperFunctions.assertionSort({})).toEqual([]);
    expect(helperFunctions.assertionSort(null)).toEqual([]);
    expect(helperFunctions.assertionSort(undefined)).toEqual([]);
  });

  test("assertionSort sorting is working as expected", () => {
    const mockData: any[] = [
      {
        id: "14b24095-6edc-4d21-ade6-171867f8c584",
        name: "Please provide a configuration screenshot showing LaunchPad is integrated with OneLogin. ",
        severity: "MEDIUM",
        section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        type: "file upload",
        options: [
          {
            value: "",
            score: 0
          }
        ],
        verifiable: false,
        number: 1,
        required: false,
        tag_ids: []
      },
      {
        id: "14b24095-6edc-4d21-ade6-171867f8c584",
        name: "At nos hinc posthac, sitientis piros Afros.",
        severity: "MEDIUM",
        section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        type: "file upload",
        options: [
          {
            value: "",
            score: 0
          }
        ],
        verifiable: false,
        number: 3,
        required: false,
        tag_ids: []
      },
      {
        id: "14b24095-6edc-4d21-ade6-171867f8c584",
        name: "Curabitur blandit tempus ardua ridiculus sed magna.",
        severity: "MEDIUM",
        section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        type: "file upload",
        options: [
          {
            value: "",
            score: 0
          }
        ],
        verifiable: false,
        number: 2,
        required: false,
        tag_ids: []
      }
    ];
    const output: any[] = [
      {
        id: "14b24095-6edc-4d21-ade6-171867f8c584",
        name: "Please provide a configuration screenshot showing LaunchPad is integrated with OneLogin. ",
        severity: "MEDIUM",
        section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        type: "file upload",
        options: [
          {
            value: "",
            score: 0
          }
        ],
        verifiable: false,
        number: 1,
        required: false,
        tag_ids: []
      },
      {
        id: "14b24095-6edc-4d21-ade6-171867f8c584",
        name: "Curabitur blandit tempus ardua ridiculus sed magna.",
        severity: "MEDIUM",
        section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        type: "file upload",
        options: [
          {
            value: "",
            score: 0
          }
        ],
        verifiable: false,
        number: 2,
        required: false,
        tag_ids: []
      },
      {
        id: "14b24095-6edc-4d21-ade6-171867f8c584",
        name: "At nos hinc posthac, sitientis piros Afros.",
        severity: "MEDIUM",
        section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        type: "file upload",
        options: [
          {
            value: "",
            score: 0
          }
        ],
        verifiable: false,
        number: 3,
        required: false,
        tag_ids: []
      }
    ];
    expect(helperFunctions.assertionSort(mockData)).toStrictEqual(output);
  });

  test("extractAnswer shouldn't break if null|undefined is passed as data", () => {
    const expectedOutput = {
      userEmail: "",
      createdOn: null,
      comments: []
    };
    expect(helperFunctions.extractAnswer({})).toStrictEqual(expectedOutput);
    expect(helperFunctions.extractAnswer(null)).toStrictEqual(expectedOutput);
    expect(helperFunctions.extractAnswer(undefined)).toStrictEqual(expectedOutput);
  });
  test("extractAnswer should correct values", () => {
    const expectedOutput = {
      userEmail: userEmail,
      createdOn: "Feb 03, 2021",
      comments: comments
    };
    expect(helperFunctions.extractAnswer(mockAnswer)).toStrictEqual(expectedOutput);
  });

  test("checkShowComments should correct value", () => {
    const sectionId = "4d21-ade6-171867f8c584";
    const questionId = "14b24095-6edc-4d21-ade6-171867f8c584";
    const mockData = {
      section_id: sectionId,
      question_id: questionId
    };

    expect(helperFunctions.checkShowComments(mockData, "", "", false)).toEqual(false);
    expect(helperFunctions.checkShowComments(mockData, "", "", true)).toEqual(true);
    expect(helperFunctions.checkShowComments(mockData, sectionId, "", false)).toEqual(false);
    expect(helperFunctions.checkShowComments(mockData, "", questionId, false)).toEqual(false);
    expect(helperFunctions.checkShowComments(mockData, sectionId, questionId, false)).toEqual(true);
    expect(helperFunctions.checkShowComments(mockData, sectionId, questionId, true)).toEqual(true);
  });

  test("getCheckListValues should correct value", () => {
    expect(helperFunctions.getCheckListValues(mockAnswer)).toStrictEqual([
      '{"value":true,"score":0}',
      '{"value":true,"score":0}'
    ]);
  });

  test("getMultiselectValues should correct value", () => {
    expect(helperFunctions.getMultiselectValues(mockAnswer)).toStrictEqual([
      '{"value":"https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf","score":0,"original_value":"91273hk12398"}',
      '{"value":"http://www.africau.edu/images/default/sample.pdf","score":0,"original_value":"73hk12398"}'
    ]);
  });

  test("multiselectValue should correct value", () => {
    expect(helperFunctions.multiselectValue({ updatedValue: undefined, value: "" }, mockAnswer)).toEqual("");
    expect(
      helperFunctions.multiselectValue(
        {
          updatedValue: undefined,
          value: "91273hk12398"
        },
        mockAnswer
      )
    ).toEqual("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
  });

  test("singleSelectOriginalValue should correct value", () => {
    expect(helperFunctions.singleSelectOriginalValue(undefined, mockAnswer)).toEqual(undefined);
    expect(helperFunctions.singleSelectOriginalValue({}, mockAnswer)).toEqual("91273hk12398");
  });

  test("singleSelectValue should correct value", () => {
    expect(helperFunctions.singleSelectValue({ updatedValue: "qwerty", value: "" }, "", "")).toEqual("qwerty");
    expect(helperFunctions.singleSelectValue({ updatedValue: undefined, value: "" }, "", "original_value")).toEqual("");
    expect(
      helperFunctions.singleSelectValue(
        {
          updatedValue: undefined,
          value: "original_value"
        },
        "",
        "original_value"
      )
    ).toEqual("original_value");
  });

  test("extractResponse should correct value", () => {
    expect(
      helperFunctions.extractResponse({
        value: "someString",
        file_name: "testFile",
        upload: false
      })
    ).toStrictEqual({
      fileId: "someString",
      fileName: "testFile",
      uploadNeeded: false
    });
  });

  test("getPrevCreatedAt should correct value", () => {
    expect(helperFunctions.getPrevCreatedAt(mockAnswer, "", {}, false, "test@gmail.com")).toStrictEqual({
      prevCreatedAt: undefined,
      newAnswer: mockAnswer
    });

    expect(helperFunctions.getPrevCreatedAt(mockAnswer, mockQuestionId, {}, false, "test@gmail.com")).toStrictEqual({
      prevCreatedAt: undefined,
      newAnswer: mockAnswer
    });
  });

  test("getResponse should correct value", () => {
    expect(helperFunctions.getResponse(undefined, 12, "123123", false)).toStrictEqual({
      value: "",
      file_name: "",
      type: "file",
      score: 0,
      upload: false
    });

    const file = {
      name: "File"
    };
    expect(helperFunctions.getResponse(file, 12, "123123", false)).toStrictEqual({
      value: file,
      file_name: "File",
      score: 0,
      type: "file",
      upload: true
    });
  });

  test("modifyQuiz should correct value", () => {
    expect(helperFunctions.modifyQuiz({}, "kj12h31u", mockAnswer)).toStrictEqual({});

    const mockOutput = {
      answered_questions: 1,
      assignment_message: "",
      completed: false,
      generation: "1612347634979627",
      id: "693f8a41-f767-4ea2-85f0-d799466a9333",
      kb_ids: [],
      product_id: "71",
      qtemplate_name: "AC-03-Q-5-1",
      questionnaire_id: "4fcb3146-4062-4aba-8aa8-413982e6b3c6",
      section_responses: [
        {
          answers: mockResponses[0].answers,
          section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6"
        }
      ],
      sections: [
        {
          created_at: 1603224688,
          description: "testing gfd sgf",
          id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
          name:
            "Password policy for length, complexity and lifetime is enforced for all access accounts for the environment.",
          questions: [
            {
              id: "14b24095-6edc-4d21-ade6-171867f8c584",
              name: "Please provide a configuration screenshot showing LaunchPad is integrated with OneLogin. ",
              number: 1,
              options: [
                {
                  score: 0,
                  value: ""
                }
              ],
              required: false,
              section_id: "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
              severity: "MEDIUM",
              tag_ids: [],
              type: "file upload",
              verifiable: false
            }
          ],
          tags: [],
          type: "DEFAULT"
        }
      ],
      sender_email: "thanh@levelops.io",
      target_email: "thanh@levelops.io",
      total_questions: 1,
      work_item_id: "8bd2a232-bcd3-4dab-bf69-74a7e07ec486"
    };

    expect(
      helperFunctions.modifyQuiz(mockQuiz, "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6", mockResponses[0])
    ).toStrictEqual(mockOutput);
  });

  test("getCalculatedRiskScore should correct value", () => {
    expect(helperFunctions.getCalculatedRiskScore([])).toStrictEqual({ answered: 0, score: 0 });

    expect(helperFunctions.getCalculatedRiskScore(mockResponses)).toStrictEqual({ answered: 1, score: 0 });
  });

  test("getAssertion should return null if question if doesn't match", () => {
    expect(helperFunctions.getAssertion([], "mockAnswer", "")).toStrictEqual(null);
  });

  test("getAssertion should correct value", () => {
    expect(
      helperFunctions.getAssertion(
        mockResponses,
        "00d119e6-d8ec-4b2b-88b4-59cd26d4f6c6",
        "14b24095-6edc-4d21-ade6-171867f8c584"
      )
    ).toStrictEqual({
      comments: [],
      question_id: "14b24095-6edc-4d21-ade6-171867f8c584",
      user_email: "hemant.saini@devslane.com",
      answered: true,
      upload: false,
      responses: [
        {
          value: "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
          score: 0,
          type: "link"
        },
        {
          value: "http://www.africau.edu/images/default/sample.pdf",
          score: 0,
          type: "link"
        }
      ],
      created_at: 1612347634
    });
  });
});
