import { DownloadAssessment } from "assessments/components";
import { getFileBlob, pdfBlob } from "assessments/utils";
import { forEach, get } from "lodash";
import React, { useCallback, useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { filesGet } from "reduxConfigs/actions/restapi/fileActions";
import JSZip from "jszip";
import { getGenericMethodSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import FileSaver from "file-saver";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { genericList } from "reduxConfigs/actions/restapi";
import { AntButton } from "shared-resources/components";
import { Checkbox, notification } from "antd";
import { CheckboxChangeEvent } from "antd/lib/checkbox";
import AssessmentTable from "assessments/components/assessment-table/assessment-table";

interface AssessmentsListPageProps {
  moreFilters: any;
}

const AssessmentsListPage: React.FC<AssessmentsListPageProps> = (props: AssessmentsListPageProps) => {
  const [selectedIds, setSelectedIds] = useState<any[]>([]);
  const [fileIds, setFileIds] = useState<any[]>([]);
  const [quizzesLoading, setQuizzesLoading] = useState<boolean>(false);
  const [filesLoading, setFilesLoading] = useState<boolean>(false);
  const [reportReady, setReportReady] = useState<boolean>(false);
  const [includeArtifacts, setIncludeArtifacts] = useState<boolean>(false);
  const [firstPdfRender, setFirstPdfRender] = useState<boolean>(false);
  const dispatch = useDispatch();
  const quizListState = useParamSelector(getGenericMethodSelector, { uri: "quiz", method: "list" });
  const quizGetState = useParamSelector(getGenericMethodSelector, { uri: "quiz", method: "get" });
  const filesGetState = useParamSelector(getGenericMethodSelector, { uri: "files", method: "get" });

  useEffect(() => {
    return () => {
      dispatch(restapiClear("quiz", "list", "0"));
      dispatch(restapiClear("tags", "bulk", "0"));
    };
  }, []);

  useEffect(() => {
    const quizList = get(quizListState, ["data", "records"], []);
    if (quizzesLoading) {
      let quizLoading = false;
      let filesLoading = false;
      let fileIds: any = [];
      let firstpdfRender = firstPdfRender;
      selectedIds.forEach((id: any) => {
        const loading = get(quizGetState, [id.toString(), "loading"], true);
        const error = get(quizGetState, [id.toString(), "error"], false);
        if (loading || error) {
          quizLoading = true;
        } else {
          if (!firstpdfRender) {
            firstpdfRender = true;
            const quiz = get(quizGetState, [id.toString(), "data"], {});
            const quizpdf = <DownloadAssessment assessment={quiz} />;
            pdfBlob(quizpdf);
          }
        }
      });

      if (!quizLoading && includeArtifacts) {
        forEach(selectedIds, (id: any) => {
          const quiz = get(quizGetState, [id.toString(), "data"], {});
          const quizFromList = quizList.find((quiz: any) => quiz.id === id);
          forEach(quiz?.section_responses || [], (section: any) => {
            forEach(section?.answers || [], (answer: any) => {
              if (answer?.answered && answer?.responses?.length > 0) {
                if (answer.responses[0]?.type === "file") {
                  filesLoading = true;
                  let fetchFileId = `quiz/${quiz.id}/assertion/${answer.question_id}/${answer.responses[0]?.value}`;
                  const fileName = quizFromList?.vanity_id
                    ? `${quizFromList.vanity_id}_${answer.responses[0]?.file_name}`
                    : answer.responses[0]?.file_name;
                  fileIds.push({
                    id: fetchFileId,
                    name: fileName
                  });
                  dispatch(filesGet(fetchFileId, fileName, false));
                }
              }
            });
          });
        });
      }
      setQuizzesLoading(quizLoading);
      setFilesLoading(filesLoading);
      setFileIds(fileIds);
      setReportReady(!quizLoading && !filesLoading);
      setFirstPdfRender(firstpdfRender);
    }
  }, [quizGetState, quizListState]);

  useEffect(() => {
    if (filesLoading) {
      let loading = false;
      forEach(fileIds, (obj: any) => {
        const curLoading = get(filesGetState, [obj.id, "loading"], true);
        const curError = get(filesGetState, [obj.id, "error"], false);
        if (curLoading) {
          loading = true;
        } else {
          const file = get(filesGetState, [obj.id, "data"], {});
          if (!curError) {
            if (file === undefined) {
              loading = true;
            }
          }
        }
      });
      setFilesLoading(loading);
      setReportReady(!loading);
    }
  }, [filesGetState]);

  useEffect(() => {
    const quizList = get(quizListState, ["data", "records"], []);
    if (reportReady && firstPdfRender) {
      let zip = new JSZip();
      let folder = zip.folder("assessments");
      (async () => {
        forEach(selectedIds, (id: any) => {
          const quiz = get(quizGetState, [id, "data"], {});
          const quizFromList = quizList.find((quiz: any) => quiz.id === id);
          const quizPDF = <DownloadAssessment assessment={quiz} />;
          const fileName = quizFromList.vanity_id
            ? `${quizFromList.vanity_id}_${quiz.qtemplate_name}`
            : `${quiz.qtemplate_name}`;
          folder?.file(`${fileName}.pdf`, pdfBlob(quizPDF));
        });
      })();
      forEach(fileIds, (obj: any) => {
        folder?.file(obj.name, getFileBlob(get(filesGetState, [obj.id, "data"], {})));
      });
      zip.generateAsync({ type: "blob" }).then(content => FileSaver.saveAs(content, "assessments.zip"));
      dispatch(restapiClear("files", "get", "-1"));
      dispatch(restapiClear("quiz", "get", "-1"));
      setReportReady(false);
      setFileIds([]);
    }
  }, [quizGetState, quizListState, filesGetState]);

  const handleSelectRows = useCallback((ids: any[]) => {
    setSelectedIds(ids);
  }, []);

  const generateReport = useCallback(() => {
    const reportsToDownload = selectedIds.map(id => ({
      id: id,
      include_artifacts: includeArtifacts
    }));
    dispatch(genericList("assessment_download", "list", reportsToDownload, null, "0"));
    notification.info({ message: `Getting ${selectedIds.length > 1 ? `assessments` : `assessment`}...` });
  }, [selectedIds, includeArtifacts]);

  return (
    <>
      <Checkbox checked={includeArtifacts} onChange={(e: CheckboxChangeEvent) => setIncludeArtifacts(e.target.checked)}>
        Include Artifacts
      </Checkbox>
      <AntButton onClick={generateReport} className="mb-10 ml-10" disabled={selectedIds.length === 0} icon={"download"}>
        Download Assessments
      </AntButton>
      <AssessmentTable moreFilters={props.moreFilters || {}} partialFilters={{}} onSelectRows={handleSelectRows} />
    </>
  );
};

export default AssessmentsListPage;
