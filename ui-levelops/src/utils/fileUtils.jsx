export const supportedExtensions = [
  "aac",
  "ai",
  "avi",
  "bmp",
  "cad",
  "cdr",
  "css",
  "dat",
  "dll",
  "dmg",
  "doc",
  "eps",
  "fla",
  "flv",
  "gif",
  "html",
  "indd",
  "iso",
  "js",
  "midi",
  "mov",
  "mp3",
  "mpg",
  "pdf",
  "php",
  "png",
  "ppt",
  "ps",
  "psd",
  "raw",
  "sql",
  "svg",
  "tif",
  "txt",
  "wmv",
  "xls",
  "xml",
  "zip"
];

export const canBePreviewed = fileName => {
  const items = fileName.split(".");

  if (!items.length) {
    return false;
  }

  const _extension = items[items.length - 1];

  switch (_extension) {
    case "png":
    case "jpg":
    case "jpeg":
    case "svg": {
      return true;
    }

    default: {
      return false;
    }
  }
};

export const filePlaceholderIcon = fileName => {
  const items = fileName.split(".");

  if (!items.length) {
    return "rawfile";
  }

  const _extension = items[items.length - 1];

  if (supportedExtensions.includes(_extension)) {
    return _extension + "file";
  }

  switch (_extension) {
    case "3ds":
      return "threedsfile";
    case "jpg":
    case "jpeg":
      return "jpgfile";
    default:
      return "rawfile";
  }
};
