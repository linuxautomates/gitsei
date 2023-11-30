import React, { useEffect, useState } from "react";
import { fetchProtectedImage } from "../../../utils/restRequest";
import { SvgIcon } from "../index";
import "./AuthImage.style.scss";

interface AuthImageProps {
  src?: string;
  className?: string;
  alt?: string;
}

const AuthImage: React.FC<AuthImageProps> = ({ className, src, alt }) => {
  const [srcUrl, setSrcUrl] = useState<undefined | string>(undefined);
  const [loadingImg, setLoadingImg] = useState<boolean>(true);

  const downloadImage = async () => {
    if (src) {
      try {
        setLoadingImg(true);
        const blobUrl = await fetchProtectedImage(src);
        setLoadingImg(false);
        setSrcUrl(blobUrl);
      } catch (e) {
        console.error(e);
      }
    } else {
      setLoadingImg(false);
    }
  };

  useEffect(() => {
    downloadImage();
  }, []);

  if (loadingImg) {
    return null;
  }

  if (!src) {
    return <SvgIcon className={`auth-image-placeholder ${className}`} icon="reportPlaceholder" />;
  }

  return <img className={className} src={srcUrl} alt={alt} />;
};

export default React.memo(AuthImage);
