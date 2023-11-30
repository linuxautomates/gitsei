import * as React from "react";

import "./previewer.style.scss";
import { AntModalComponent as AntModal } from "../ant-modal/ant-modal.component";
import { Icon } from "antd";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { useDispatch, useSelector } from "react-redux";
import Loader from "../../../components/Loader/Loader";
import { canBePreviewed, filePlaceholderIcon } from "../../../utils/fileUtils";
import { SvgIconComponent as SvgIcon } from "../svg-icon/svg-icon.component";
import { useEffect, useState } from "react";
import { filesHead } from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { get } from "lodash";

interface PreviewerProps {
  onClose: (e: any) => void;
  onDownload: (event: any, id: any, name: any) => void;
  list: any[];
  currentIndex: number;
}

const PreviewerComponent: React.FC<PreviewerProps> = props => {
  const dispatch = useDispatch();
  const filesState = useSelector(state => get(state, ["restapiReducer", "files", "head"], {}));

  const [index, setIndex] = useState(props.currentIndex);
  const [selectedItem, setSelectedItem] = useState(props.list[props.currentIndex]);
  const [data, setData] = useState<any>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkForCache(props.list[props.currentIndex]);

    return () => {
      dispatch(restapiClear("files", "head", "-1"));
      dispatch(restapiClear("files", "get", "-1"));
    };
  }, []);

  useEffect(() => {
    if (loading) {
      const fileId = selectedItem.fetchFileId;
      const fileName = selectedItem.fileName;

      if (data[fileId] || !canBePreviewed(fileName)) {
        setLoading(false);
        return;
      }

      const { loading: _loading, error } = get(filesState, fileId, { loading: true, error: true });
      if (!_loading && _loading !== undefined && !error && error !== undefined) {
        const apiData = filesState[fileId].data;

        setLoading(false);
        setData((data: any) => ({
          ...data,
          [fileId]: apiData
        }));
      }
    }
  }, [filesState, loading]);

  const checkForCache = (item: any) => {
    if (!item) {
      return;
    }

    const { fetchFileId, fileName } = item;

    if (!data[fetchFileId] && canBePreviewed(fileName)) {
      dispatch(filesHead(fetchFileId));
    }
  };

  const moveLeft = () => {
    checkForCache(props.list[index - 1]);

    setIndex(index => index - 1);
    setSelectedItem(props.list[index - 1]);
    setLoading(true);
  };

  const moveRight = () => {
    checkForCache(props.list[index + 1]);

    setIndex(index => index + 1);
    setSelectedItem(props.list[index + 1]);
    setLoading(true);
  };

  const getIcon = () => {
    const _canbePreviewed = canBePreviewed(selectedItem.fileName);
    if (_canbePreviewed) {
      return "file-image";
    } else {
      return "file-text";
    }
  };

  const getFileType = () => {
    const _canBePreviewed = canBePreviewed(selectedItem.fileName);
    if (_canBePreviewed) {
      return "image";
    } else {
      return "document";
    }
  };

  const { onClose, onDownload } = props;
  return (
    <AntModal
      className={"preview-modal"}
      width="100%"
      style={{ top: 0, left: 0, color: "rgb(184, 199, 224)", backgroundColor: "transparent" }}
      bodyStyle={{ height: "100vh", color: "rgb(184, 199, 224)", backgroundColor: "transparent" }}
      visible={true}
      closeIcon={<></>}
      footer={null}>
      <div className="previewer-header">
        <Icon style={{ fontSize: "24px", padding: "1rem" }} type={getIcon()} />
        <div>
          <div style={{ fontWeight: "bold" }}>{props.list[index].fileName}</div>
          <div>{getFileType()}</div>
        </div>
        <div style={{ display: "flex", flex: "1 1 auto" }} />
        <div>
          <Icon
            style={{ fontSize: "24px", marginRight: "1rem" }}
            type="download"
            onClick={e => onDownload(e, props.list[index].fetchFileId, props.list[index].fileName)}
          />
          <Icon style={{ fontSize: "24px" }} type="close" onClick={e => onClose(e)} />
        </div>
      </div>
      <div className="nav-buttons">
        {index > 0 && (
          <Icon className="nav-buttons__button" theme="filled" type="left-circle" onClick={e => moveLeft()} />
        )}
        <div style={{ display: "flex", flex: "1 1 auto" }} />
        {index < props.list.length - 1 && (
          <Icon className="nav-buttons__button" theme="filled" type="right-circle" onClick={e => moveRight()} />
        )}
      </div>
      <div className="previewer-content">
        {loading && <Loader />}
        {!loading && canBePreviewed(selectedItem.fileName) && (
          <img width="100%" height="100%" src={data[selectedItem.fetchFileId]} />
        )}
        {!loading && !canBePreviewed(selectedItem.fileName) && (
          <span style={{ display: "flex", flexDirection: "column", justifyContent: "center" }}>
            <SvgIcon icon={filePlaceholderIcon(selectedItem.fileName)} style={{ width: 132, height: 132 }} />
          </span>
        )}
      </div>
    </AntModal>
  );
};

export default PreviewerComponent;
