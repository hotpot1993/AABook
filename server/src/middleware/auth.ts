import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

const JWT_SECRET = process.env.JWT_SECRET || 'change-me';

export interface AuthRequest extends Request {
  userId?: number;
  nickname?: string;
}

export function generateToken(userId: number, nickname: string): string {
  return jwt.sign({ userId, nickname }, JWT_SECRET, { expiresIn: '30d' });
}

export function authMiddleware(req: AuthRequest, res: Response, next: NextFunction): void {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    res.status(401).json({ error: '未登录' });
    return;
  }
  try {
    const payload = jwt.verify(header.substring(7), JWT_SECRET) as { userId: number; nickname: string };
    req.userId = payload.userId;
    req.nickname = payload.nickname;
    next();
  } catch {
    res.status(401).json({ error: '登录过期' });
  }
}
