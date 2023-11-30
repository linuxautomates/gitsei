/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

import { Container } from '@harness/uicore'
import React from 'react'
import classNames from 'classnames'

import css from './CardV2.module.scss'

interface CardProps {
  children: React.ReactNode
  className?: string
  contentClassName?: string
}

export default function CardV2({ children, className, contentClassName }: CardProps): JSX.Element {
  return (
    <Container className={classNames(className, css.container)}>
      <Container className={classNames(contentClassName, css.content)}>{children}</Container>
    </Container>
  )
}
