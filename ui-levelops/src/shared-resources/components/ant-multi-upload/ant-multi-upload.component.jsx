import React from "react";
import * as PropTypes from "prop-types";
import { Button, Modal, Upload } from "antd";
import "./ant-multi-upload.styles.scss";

export class AntMultiUploadComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      filesInfo: [],
      previewInfo: null
    };
  }

  handleAddFiles = (file, files) => {
    this.props.onAddFile(file);
  };

  handleRemove = file => {
    this.props.onRemove(file);
  };

  get previewModal() {
    const { previewInfo } = this.state;
    if (!previewInfo) {
      return null;
    }
    const { name, url } = previewInfo;
    return (
      <Modal
        visible={!!previewInfo}
        title={name}
        footer={null}
        onCancel={() => {
          this.setState({ previewInfo: null });
        }}>
        <img className="preview-img" src={url} alt="previewImg" />
      </Modal>
    );
  }

  get files() {
    return this.props.files || [];
  }

  render() {
    return (
      <>
        <Upload
          multiple={this.props.multiple}
          fileList={this.files}
          onRemove={this.handleRemove}
          onDownload={file => this.props.onDownloadFile(file.upload_id)}
          showUploadList={{
            showRemoveIcon: this.props.showRemove,
            showDownloadIcon: true
          }}
          beforeUpload={(file, fileList) => {
            this.handleAddFiles(file, fileList);
            return false;
          }}>
          <Button disabled={this.props.disableFileUpload} type="link" icon="upload">
            Upload
          </Button>
        </Upload>
      </>
    );
  }
}

AntMultiUploadComponent.propTypes = {
  files: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
  disableFileUpload: PropTypes.bool,
  onFilesUploaded: PropTypes.func,
  onAddFile: PropTypes.func.isRequired,
  onDownloadFile: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
  showRemove: PropTypes.bool.isRequired,
  multiple: PropTypes.bool.isRequired
};

AntMultiUploadComponent.defaultProps = {
  files: [],
  disableFileUpload: false,
  onRemove: () => {},
  showRemove: false,
  multiple: false
};
