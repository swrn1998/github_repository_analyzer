package com.githubanalyzer.service.alert;

import com.githubanalyzer.domain.Alert;
import com.githubanalyzer.dto.internal.RepoData;

import java.util.List;

public interface AlertEngine {

    List<Alert> evaluate(RepoData repoData);
}
