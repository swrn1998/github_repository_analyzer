package com.githubanalyzer.service.alert.rule;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.dto.internal.RepoData;

public interface AlertRule {

    boolean applies(RepoData repoData);

    Alert generateAlert(RepoData repoData);
}
