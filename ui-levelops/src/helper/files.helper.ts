import { notification } from "antd";
import { FAILURE_UPLOAD_FILE, FILE_TYPE_NOT_ALLOWED } from "constants/error.constants";
import { RESTRICTED_EXTENSIONS } from "constants/files";

/**
 * Checks whether or not a file is allowed based on the extension
 *
 * @param fullFileName The full file name; something.txt
 * @returns
 */
export const isFileTypeAllowed = (fullFileName: string): boolean => {
  try {
    let fileParts = fullFileName.split(".");
    let extension = fileParts[fileParts.length - 1];

    if (!RESTRICTED_EXTENSIONS.includes(extension)) {
      return true;
    }
  } catch (err) {}
  return false;
};

export const showTypeNotAllowedMessage = (): void => notification.error({ message: FILE_TYPE_NOT_ALLOWED });
// export const showFailureToUpload = (): void => notification.error({ message: FAILURE_UPLOAD_FILE });
