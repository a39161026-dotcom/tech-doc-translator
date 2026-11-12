package com.techdoc.translator.summary;

import com.techdoc.translator.document.SummaryLength;
import com.techdoc.translator.document.SummaryStatus;

public record SummaryResponse(SummaryStatus status, SummaryLength length, String text) {
}
