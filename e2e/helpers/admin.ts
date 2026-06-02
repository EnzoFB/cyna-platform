import { refreshSessionTokens, registerUser, RegisteredUser } from './api';
import { promoteUserToAdmin } from './db';

export interface AdminFixture extends RegisteredUser {
  adminAccessToken: string;
  adminRefreshToken: string;
}

export async function createAdminUser(suffix = ''): Promise<RegisteredUser> {
  const user = await registerUser(suffix);
  await promoteUserToAdmin(user.email);
  return user;
}

export async function createAdminFixture(suffix = ''): Promise<AdminFixture> {
  const user = await createAdminUser(suffix);
  const tokens = await refreshSessionTokens(user.refreshToken);

  return {
    ...user,
    adminAccessToken: tokens.accessToken,
    adminRefreshToken: tokens.refreshToken,
  };
}
