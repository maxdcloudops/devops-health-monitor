package com.derbenev.monitor.exception;

import com.derbenev.monitor.model.ProposalStatus;

public class ProposalAlreadyDecidedException extends RuntimeException {

    public ProposalAlreadyDecidedException(Long id, ProposalStatus status) {
        super("Trade proposal id=" + id + " already decided: " + status);
    }
}
