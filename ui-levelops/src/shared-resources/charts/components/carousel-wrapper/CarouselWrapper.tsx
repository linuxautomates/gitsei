import React, { createRef, useCallback, useEffect, useMemo, useState } from "react";
import { AntButton } from "../../../components";

interface CarouselWrapperProps {
  className?: string;
  refresh: number;
}

const CarouselWrapper: React.FC<CarouselWrapperProps> = props => {
  const { className, children, refresh } = props;

  const [width, setWidth] = useState(0);
  const [scrollWidth, setScrollWidth] = useState(0);
  const [scrollPosition, setScrollPosition] = useState(0);

  const childRef = createRef<HTMLDivElement>();

  useEffect(() => {
    if (childRef.current) {
      childRef.current.scrollTo({ left: 0, behavior: "smooth" });
      setScrollPosition(0);
    }
  }, [refresh]);

  useEffect(() => {
    if (childRef && childRef.current) {
      setWidth(childRef.current.clientWidth);
      setScrollWidth(childRef.current.scrollWidth);
    }
  }, [childRef]);

  const handleNext = useCallback(() => {
    if (childRef.current) {
      const pos = childRef.current.scrollLeft + childRef.current.clientWidth / 2;
      if (childRef.current.scrollLeft + childRef.current.clientWidth === childRef.current.scrollWidth) {
        setScrollPosition(childRef.current.scrollWidth);
      } else {
        setScrollPosition(pos);
      }
      childRef.current.scrollTo({ left: pos, behavior: "smooth" });
    }
  }, [childRef]);

  const handlePrev = useCallback(() => {
    if (childRef.current) {
      const pos = childRef.current.scrollLeft - childRef.current.clientWidth / 2;
      childRef.current.scrollTo({ left: pos, behavior: "smooth" });
      setScrollPosition(pos);
    }
  }, [childRef]);

  const hideNavButtons = useMemo(() => {
    return scrollWidth < width;
  }, [width, scrollWidth]);

  const disableNext = useMemo(() => {
    return scrollPosition >= scrollWidth;
  }, [scrollPosition, scrollWidth]);

  const disablePrev = useMemo(() => {
    return scrollPosition <= 0;
  }, [scrollPosition]);

  return (
    <div className="phase-carousel">
      {!hideNavButtons && (
        <div className="nav-button-container nav-button-container-left">
          <AntButton className="prev-btn" icon="left" shape="circle" disabled={disablePrev} onClick={handlePrev} />
        </div>
      )}
      <div className={className} ref={childRef}>
        {children}
      </div>
      {!hideNavButtons && (
        <div className="nav-button-container nav-button-container-right">
          <AntButton className="next-btn" icon="right" shape="circle" disabled={disableNext} onClick={handleNext} />
        </div>
      )}
    </div>
  );
};

export default CarouselWrapper;
