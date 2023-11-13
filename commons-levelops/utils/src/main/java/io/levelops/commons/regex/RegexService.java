package io.levelops.commons.regex;

import com.google.common.base.Preconditions;
import io.levelops.commons.models.RegexResult;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class RegexService {
    private Integer blobSize = 1500;

    public RegexService(Integer blobSize) {
        this.blobSize = blobSize;
    }

    //for defaults
    public RegexService() {
    }

    public RegexResult findRegexHits(final Set<String> regexes, final String textBlob) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(regexes), "regex list cannot be empty.");
        Preconditions.checkArgument(StringUtils.isNotEmpty(textBlob), "Textblob cannot be empty.");
        ImmutablePair<Integer, Integer> contextIndices = null;
        final Map<String, Integer> regexMatchCount = new HashMap<>();
        for (String regex : regexes) {
            int hitCountForRegex = 0;
            Pattern compiled = Pattern.compile(regex);
            Matcher matcher = compiled.matcher(textBlob);
            while (matcher.find()) {
                hitCountForRegex += 1;
                if (contextIndices == null)
                    contextIndices = ImmutablePair.of(matcher.start(), matcher.end());
            }
            regexMatchCount.put(regex, hitCountForRegex);
        }
        ImmutablePair<String, Integer> context = contextIndices == null ?
                ImmutablePair.nullPair() : getContextBlob(textBlob, contextIndices);
        return RegexResult.builder()
                .firstHitContext(context.getLeft())
                .firstHitLineNumber(context.getRight())
                .totalMatches(regexMatchCount.values().stream().reduce(0, Integer::sum))
                .regexCount(regexMatchCount)
                .build();
    }

    public ImmutablePair<String, Integer> getContextBlob(final String textBlob,
                                                         ImmutablePair<Integer, Integer> indices) {
        int leftIndex = 0;
        int rightIndex = textBlob.length();
        if ((indices.getLeft() - blobSize / 2) > 0) {
            leftIndex = indices.getLeft() - blobSize / 2;
            int tmpLeftIndex = Math.max(leftIndex - blobSize / 2, 0);
            if (textBlob.substring(tmpLeftIndex, leftIndex).contains("\n"))
                leftIndex = tmpLeftIndex + textBlob.substring(tmpLeftIndex, leftIndex).indexOf('\n') + 1;
        }

        if ((indices.getRight() + blobSize / 2) < textBlob.length()) {
            rightIndex = indices.getRight() + blobSize / 2;
            int tmpRightIndex = Math.min(rightIndex + blobSize / 2, textBlob.length());
            if (textBlob.substring(rightIndex, tmpRightIndex).contains("\n"))
                rightIndex = rightIndex + textBlob.substring(rightIndex, tmpRightIndex).indexOf('\n') + 1;
        }
        return ImmutablePair.of(
                textBlob.substring(leftIndex, rightIndex), // get the blob
                textBlob.substring(0, leftIndex) // get the line number
                        .split("\n").length);
    }
}
