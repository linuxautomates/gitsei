import React, { createRef, ReactNode, useCallback, useEffect, useMemo, useState } from "react";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";

interface CarouselWrapperProps {
  className?: string;
  refresh: number;
  data: any;
}

const EffortInvestmentCarouselWrapper: React.FC<CarouselWrapperProps> = props => {
  const { className, children, refresh, data } = props;
  const [currentIndex, setCurrentIndex] = useState<number>(0);

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
    if (
      childRef &&
      childRef.current &&
      childRef.current.scrollWidth &&
      (childRef.current.clientWidth !== width || childRef.current.scrollWidth !== scrollWidth)
    ) {
      setWidth(childRef.current.clientWidth);
      setScrollWidth(childRef.current.scrollWidth);
    }
  }, [childRef, width]);

  const handleNext = useCallback(() => {
    if (childRef.current) {
      const pos = (currentIndex + 1) * childRef.current.clientWidth;
      if (childRef.current.scrollLeft + childRef.current.clientWidth === childRef.current.scrollWidth) {
        setScrollPosition(childRef.current.scrollWidth);
      } else {
        setScrollPosition(pos);
      }
      childRef.current.scrollTo({ left: pos, behavior: "smooth" });
      setCurrentIndex(state => state + 1);
    }
  }, [childRef, setCurrentIndex, currentIndex]);

  const handlePrev = useCallback(() => {
    if (childRef.current) {
      let pos = childRef.current.scrollWidth - (getTotalDots - (currentIndex - 1)) * childRef.current.clientWidth;
      if (pos < 0) {
        pos = 0;
      }
      childRef.current.scrollTo({ left: pos, behavior: "smooth" });
      setScrollPosition(pos);
      setCurrentIndex(state => (state - 1 < 0 ? state : state - 1));
    }
  }, [childRef, setCurrentIndex, currentIndex]);

  const hideNavButtons = useMemo(() => scrollWidth <= width, [width, scrollWidth]);

  const disableNext = useMemo(() => scrollPosition >= scrollWidth - (width || 0), [scrollPosition, scrollWidth, width]);

  const disablePrev = useMemo(() => scrollPosition === 0, [scrollPosition, scrollWidth]);

  const getTotalDots = useMemo(() => {
    if (width === 0) {
      return 0;
    }
    const pageSize = Math.floor(width / 270);
    const totalRecords = data.length;
    return Math.ceil(totalRecords / pageSize);
  }, [data, width]);

  const getDots = useMemo(() => {
    const dotsArray: ReactNode[] = [];
    for (let index = 0; index < getTotalDots; index++) {
      dotsArray.push(
        <div
          key={`getTotalDots_${index}`}
          id={`getTotalDots_${index}`}
          className={currentIndex === index ? "custom-dot-active" : "custom-dot"}
        />
      );
    }
    return dotsArray;
  }, [getTotalDots, currentIndex]);

  return (
    <div className="phase-carousel-wrapper">
      <div className="phase-carousel">
        {!hideNavButtons && (
          <>
            <div className="nav-button-container nav-button-container__left">
              <AntButton className="prev-btn" icon="left" shape="circle" disabled={disablePrev} onClick={handlePrev} />
            </div>
            <div className="nav-button-container-overlay nav-button-container-overlay__left" />
          </>
        )}
        <div className={className} ref={childRef}>
          {children}
        </div>
        {!hideNavButtons && (
          <>
            <div className="nav-button-container nav-button-container__right">
              <AntButton className="next-btn" icon="right" shape="circle" disabled={disableNext} onClick={handleNext} />
            </div>
            <div className="nav-button-container-overlay nav-button-container-overlay__right" />
          </>
        )}
      </div>
      <div className="custom-carousal-dots">{getDots}</div>
    </div>
  );
};

export default EffortInvestmentCarouselWrapper;
