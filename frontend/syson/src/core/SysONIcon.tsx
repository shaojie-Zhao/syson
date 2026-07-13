/*******************************************************************************
 * Copyright (c) 2023, 2024 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/

export const SysONIcon = (props: any) => {
  return (
    <>
      <img src="/logo0.png" alt="RenGu" className="logo-home" style={{ height: 48, width: 'auto' }} {...props} />
      <img src="/logo1.png" alt="RenGu" className="logo-project" style={{ height: 48, width: 'auto' }} {...props} />
    </>
  );
};
