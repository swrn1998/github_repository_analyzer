import httpClient from './httpClient';

export async function analyzeRepo(owner, repo) {
  const response = await httpClient.get('/analyze', { params: { owner, repo } });
  return response.data;
}

export async function compareRepos(ownerA, repoA, ownerB, repoB) {
  const response = await httpClient.get('/compare', {
    params: { ownerA, repoA, ownerB, repoB },
  });
  return response.data;
}

export async function getTrends(owner, repo) {
  const response = await httpClient.get('/trends', { params: { owner, repo } });
  return response.data;
}

export async function getCacheStats() {
  const response = await httpClient.get('/cache/stats');
  return response.data;
}

export async function clearCache() {
  await httpClient.delete('/cache');
}
