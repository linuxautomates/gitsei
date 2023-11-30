import React, { useEffect, useState } from "react";
import { Color, Container, Text } from "@harness/uicore";

interface KeyExpirationTimerProps {
  keyExpirationTime: number;
  tokenRegenratedCount: number;
}

const KeyExpirationTimer = (props: KeyExpirationTimerProps): JSX.Element => {
  const { keyExpirationTime, tokenRegenratedCount } = props;
  const initialTimeInSeconds = keyExpirationTime;
  const [initialTime, setInitialTime] = useState<number>(initialTimeInSeconds);
  const [timeRemaining, setTimeRemaining] = useState<number>(initialTime);

  const [isKeyExpired, setIsKeyExpired] = useState<boolean>(false);

  useEffect(() => {
    // Reset the timer when the tokenRegenratedCount prop changes
    setInitialTime(initialTimeInSeconds);
    setTimeRemaining(initialTimeInSeconds);

    const intervalId = setInterval(() => {
      setTimeRemaining(prevTime => {
        if (prevTime === 0) {
          clearInterval(intervalId);
          // setting key expired true whenever countdown reaches to 0
          setIsKeyExpired(true);
        }
        return prevTime - 1;
      });
    }, 1000); // Update every 1 second

    // Cleanup interval on component unmount
    return () => clearInterval(intervalId);
  }, [initialTimeInSeconds, tokenRegenratedCount]); // Run effect when the initial time or tokenRegenratedCount changes

  // Format seconds into minutes:seconds
  const formatTime = (seconds: number): string => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    const formattedTime = `${String(minutes).padStart(2, "0")}:${String(remainingSeconds).padStart(2, "0")}`;
    return formattedTime;
  };

  return (
    <Container padding={{ left: "xlarge" }}>
      {isKeyExpired ? (
        <Text color={Color.RED_700}>{"The key has expired . Please regenerate key !"}</Text>
      ) : (
        <Text>{`The key expires in ${formatTime(
          timeRemaining
        )} minutes. Generate a new key if the current one expires.`}</Text>
      )}
    </Container>
  );
};

export default KeyExpirationTimer;
